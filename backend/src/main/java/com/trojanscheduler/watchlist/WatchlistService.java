package com.trojanscheduler.watchlist;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trojanscheduler.user.User;
import com.trojanscheduler.user.UserRepository;
import com.trojanscheduler.watchlist.dto.AddWatchlistRequest;
import com.trojanscheduler.watchlist.dto.WatchlistItemResponse;

@Service
public class WatchlistService {

	private final WatchlistRepository watchlistRepository;
	private final UserRepository userRepository;

	public WatchlistService(WatchlistRepository watchlistRepository, UserRepository userRepository) {
		this.watchlistRepository = watchlistRepository;
		this.userRepository = userRepository;
	}

	public List<WatchlistItemResponse> listByUserId(Long userId) {
		return watchlistRepository.findByUserIdOrderByPriorityDescCreatedAtAsc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public WatchlistItemResponse add(Long userId, AddWatchlistRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new WatchlistException("User not found"));
		if (watchlistRepository.existsByUserIdAndTermCodeAndSisSectionId(userId, request.termCode(), request.sisSectionId())) {
			throw new WatchlistException("Section already in watchlist");
		}
		Instant now = Instant.now();
		Watchlist w = new Watchlist(user, request.termCode(), request.sisSectionId(), request.coursePrefix(), request.priority(), now);
		w = watchlistRepository.save(w);
		return toResponse(w);
	}

	@Transactional
	public void remove(Long userId, String termCode, String sisSectionId) {
		watchlistRepository.findByUserIdAndTermCodeAndSisSectionId(userId, termCode, sisSectionId)
				.ifPresent(watchlistRepository::delete);
	}

	private WatchlistItemResponse toResponse(Watchlist w) {
		return new WatchlistItemResponse(
				w.getId(),
				w.getTermCode(),
				w.getSisSectionId(),
				w.getCoursePrefix(),
				w.getPriority(),
				w.getCreatedAt()
		);
	}
}
