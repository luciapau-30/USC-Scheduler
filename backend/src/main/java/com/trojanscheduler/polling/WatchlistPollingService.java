package com.trojanscheduler.polling;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trojanscheduler.notification.NotificationEvent;
import com.trojanscheduler.notification.NotificationEventRepository;
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
 * Fetches section data for all watched (termCode, sisSectionId) pairs, updates
 * section_snapshot, and creates notification_events when a seat opens (0 → &gt;0).
 */
@Service
public class WatchlistPollingService {

	private static final Logger log = LoggerFactory.getLogger(WatchlistPollingService.class);
	private static final String EVENT_SEAT_OPENED = "seat_opened";
	/** Time bucket (seconds) for event fingerprint to dedupe within same window. */
	private static final long FINGERPRINT_BUCKET_SECONDS = 300;

	private final WatchlistRepository watchlistRepository;
	private final SectionSnapshotRepository sectionSnapshotRepository;
	private final NotificationEventRepository notificationEventRepository;
	private final UscClient uscClient;
	private final UscClientProperties uscProperties;
	private final SeatStateService seatStateService;

	public WatchlistPollingService(
			WatchlistRepository watchlistRepository,
			SectionSnapshotRepository sectionSnapshotRepository,
			NotificationEventRepository notificationEventRepository,
			UscClient uscClient,
			UscClientProperties uscProperties,
			SeatStateService seatStateService
	) {
		this.watchlistRepository = watchlistRepository;
		this.sectionSnapshotRepository = sectionSnapshotRepository;
		this.notificationEventRepository = notificationEventRepository;
		this.uscClient = uscClient;
		this.uscProperties = uscProperties;
		this.seatStateService = seatStateService;
	}

	/**
	 * Run one poll cycle: distinct sections from watchlist, fetch each, update snapshot, emit events on seat open.
	 */
	public void runPollCycle() {
		List<Object[]> distinct = watchlistRepository.findDistinctTermCodeAndSisSectionId();
		if (distinct.isEmpty()) {
			return;
		}
		log.debug("Polling {} watched sections", distinct.size());
		for (Object[] row : distinct) {
			String termCode = (String) row[0];
			String sisSectionId = (String) row[1];
			try {
				updateSectionAndEmitEvents(termCode, sisSectionId);
			} catch (Exception e) {
				log.warn("Poll failed for section {} {}: {}", termCode, sisSectionId, e.getMessage());
			}
		}
	}

	@Transactional
	public void updateSectionAndEmitEvents(String termCode, String sisSectionId) {
		String path = buildSectionPath(termCode, sisSectionId);
		String rawJson;
		try {
			rawJson = uscClient.get(path);
		} catch (UscException e) {
			log.debug("USC fetch failed for {} {}: {}", termCode, sisSectionId, e.getMessage());
			return;
		}

		SectionSeatState state = seatStateService.parseSectionPayload(rawJson);
		Instant now = Instant.now();

		Optional<SectionSnapshot> existing = sectionSnapshotRepository.findByTermCodeAndSisSectionId(termCode, sisSectionId);
		Integer previousOpen = existing.map(SectionSnapshot::getOpenSeats).orElse(null);
		boolean seatJustOpened = (previousOpen != null && previousOpen == 0) && state.hasOpenSeats();

		if (existing.isPresent()) {
			SectionSnapshot snap = existing.get();
			snap.update(now, state.status(), state.capacity(), state.enrolled(), state.openSeats(), rawJson);
			sectionSnapshotRepository.save(snap);
		} else {
			sectionSnapshotRepository.save(new SectionSnapshot(
					termCode, sisSectionId, now, state.status(), state.capacity(), state.enrolled(), state.openSeats(), rawJson));
		}

		if (seatJustOpened) {
			byte[] fingerprint = eventFingerprint(EVENT_SEAT_OPENED, termCode, sisSectionId, now);
			List<Watchlist> entries = watchlistRepository.findByTermCodeAndSisSectionId(termCode, sisSectionId);
			for (Watchlist w : entries) {
				Long userId = w.getUser().getId();
				if (!notificationEventRepository.existsByUser_IdAndEventFingerprint(userId, fingerprint)) {
					notificationEventRepository.save(new NotificationEvent(
							w.getUser(), termCode, sisSectionId, EVENT_SEAT_OPENED, fingerprint, now));
				}
			}
		}
	}

	private String buildSectionPath(String termCode, String sisSectionId) {
		return uscProperties.getSectionPathTemplate()
				.replace("{termCode}", java.net.URLEncoder.encode(termCode, StandardCharsets.UTF_8))
				.replace("{sisSectionId}", java.net.URLEncoder.encode(sisSectionId, StandardCharsets.UTF_8));
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
