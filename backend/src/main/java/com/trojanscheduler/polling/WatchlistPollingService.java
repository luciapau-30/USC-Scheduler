package com.trojanscheduler.polling;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.trojanscheduler.notification.NotificationEvent;
import com.trojanscheduler.notification.NotificationEventRepository;
import com.trojanscheduler.notification.NotificationPublisher;
import com.trojanscheduler.notification.dto.NotificationPayload;
import com.trojanscheduler.section.SectionSeatState;
import com.trojanscheduler.section.SectionSnapshot;
import com.trojanscheduler.section.SectionSnapshotRepository;
import com.trojanscheduler.section.SeatStateService;
import com.trojanscheduler.usc.UscClient;
import com.trojanscheduler.usc.UscClientProperties;
import com.trojanscheduler.usc.UscException;
import com.trojanscheduler.watchlist.Watchlist;
import com.trojanscheduler.watchlist.WatchlistRepository;

/**
 * Option B polling: one search per (termCode, coursePrefix) fetches all sections for that course.
 * Updates section_snapshot and creates notification_events when a seat opens (0 → &gt;0).
 * Watchlist entries with non-null coursePrefix are polled; others are skipped.
 */
@Service
public class WatchlistPollingService {

	private static final Logger log = LoggerFactory.getLogger(WatchlistPollingService.class);
	private static final String EVENT_SEAT_OPENED = "seat_opened";
	private static final long FINGERPRINT_BUCKET_SECONDS = 300;

	private final WatchlistRepository watchlistRepository;
	private final SectionSnapshotRepository sectionSnapshotRepository;
	private final NotificationEventRepository notificationEventRepository;
	private final NotificationPublisher notificationPublisher;
	private final UscClient uscClient;
	private final UscClientProperties uscProperties;
	private final SeatStateService seatStateService;

	public WatchlistPollingService(
			WatchlistRepository watchlistRepository,
			SectionSnapshotRepository sectionSnapshotRepository,
			NotificationEventRepository notificationEventRepository,
			NotificationPublisher notificationPublisher,
			UscClient uscClient,
			UscClientProperties uscProperties,
			SeatStateService seatStateService
	) {
		this.watchlistRepository = watchlistRepository;
		this.sectionSnapshotRepository = sectionSnapshotRepository;
		this.notificationEventRepository = notificationEventRepository;
		this.notificationPublisher = notificationPublisher;
		this.uscClient = uscClient;
		this.uscProperties = uscProperties;
		this.seatStateService = seatStateService;
	}

	/**
	 * Run one poll cycle: distinct (termCode, coursePrefix) with non-null prefix, fetch search per group, update snapshots and emit events.
	 */
	public void runPollCycle() {
		List<Object[]> distinct = watchlistRepository.findDistinctTermCodeAndCoursePrefix();
		if (distinct.isEmpty()) {
			return;
		}
		log.debug("Polling {} (termCode, prefix) groups", distinct.size());
		for (Object[] row : distinct) {
			String termCode = (String) row[0];
			String prefix = (String) row[1];
			if (prefix == null || prefix.isBlank()) continue;
			try {
				pollByTermAndPrefix(termCode, prefix);
			} catch (Exception e) {
				log.warn("Poll failed for term {} prefix {}: {}", termCode, prefix, e.getMessage());
			}
		}
	}

	private void pollByTermAndPrefix(String termCode, String prefix) {
		String path = buildSearchByPrefixPath(termCode, prefix);
		String rawJson;
		try {
			rawJson = uscClient.get(path);
		} catch (UscException e) {
			log.debug("USC fetch failed for {} {}: {}", termCode, prefix, e.getMessage());
			return;
		}

		Map<String, SectionSeatState> stateBySectionId = seatStateService.parseSectionsFromSearchResponse(rawJson);
		List<Watchlist> entries = watchlistRepository.findByTermCodeAndCoursePrefix(termCode, prefix);

		for (Watchlist w : entries) {
			SectionSeatState state = stateBySectionId.get(w.getSisSectionId());
			if (state == null) continue;

			Optional<SectionSnapshot> existing = sectionSnapshotRepository.findByTermCodeAndSisSectionId(termCode, w.getSisSectionId());
			Integer previousOpen = existing.map(SectionSnapshot::getOpenSeats).orElse(null);
			boolean seatJustOpened = (previousOpen != null && previousOpen == 0) && state.hasOpenSeats();

			applySectionState(termCode, w.getSisSectionId(), state, null);
			if (seatJustOpened) {
				emitSeatOpened(termCode, w.getSisSectionId());
			}
		}
	}

	// NOTE: not @Transactional — this is called from within the same class so Spring AOP
	// would bypass the proxy. Spring Data's save() provides its own transaction per call.
	public void applySectionState(String termCode, String sisSectionId, SectionSeatState state, String rawJson) {
		Instant now = Instant.now();
		Optional<SectionSnapshot> existing = sectionSnapshotRepository.findByTermCodeAndSisSectionId(termCode, sisSectionId);

		if (existing.isPresent()) {
			SectionSnapshot snap = existing.get();
			snap.update(now, state.status(), state.capacity(), state.enrolled(), state.openSeats(), rawJson);
			sectionSnapshotRepository.save(snap);
		} else {
			sectionSnapshotRepository.save(new SectionSnapshot(
					termCode, sisSectionId, now, state.status(), state.capacity(), state.enrolled(), state.openSeats(), rawJson));
		}
	}

	private void emitSeatOpened(String termCode, String sisSectionId) {
		Instant now = Instant.now();
		byte[] fingerprint = eventFingerprint(EVENT_SEAT_OPENED, termCode, sisSectionId, now);
		NotificationPayload payload = new NotificationPayload(EVENT_SEAT_OPENED, termCode, sisSectionId, now);
		for (Watchlist w : watchlistRepository.findByTermCodeAndSisSectionId(termCode, sisSectionId)) {
			Long userId = w.getUser().getId();
			if (!notificationEventRepository.existsByUser_IdAndEventFingerprint(userId, fingerprint)) {
				notificationEventRepository.save(new NotificationEvent(
						w.getUser(), termCode, sisSectionId, EVENT_SEAT_OPENED, fingerprint, now));
				notificationPublisher.sendToUser(userId, payload);
			}
		}
	}

	private String buildSearchByPrefixPath(String termCode, String prefix) {
		return uscProperties.getSectionPathTemplate()
				.replace("{termCode}", java.net.URLEncoder.encode(termCode, StandardCharsets.UTF_8))
				.replace("{prefix}", java.net.URLEncoder.encode(prefix, StandardCharsets.UTF_8));
	}

	private static byte[] eventFingerprint(String eventType, String termCode, String sisSectionId, Instant now) {
		long bucket = now.getEpochSecond() / FINGERPRINT_BUCKET_SECONDS;
		String payload = eventType + ":" + termCode + ":" + sisSectionId + ":" + bucket;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return md.digest(payload.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new RuntimeException("SHA-256 unavailable", e);
		}
	}
}
