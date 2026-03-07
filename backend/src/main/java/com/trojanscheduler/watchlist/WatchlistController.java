package com.trojanscheduler.watchlist;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trojanscheduler.watchlist.dto.AddWatchlistRequest;
import com.trojanscheduler.watchlist.dto.WatchlistItemResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

	private final WatchlistService watchlistService;

	public WatchlistController(WatchlistService watchlistService) {
		this.watchlistService = watchlistService;
	}

	@GetMapping
	public ResponseEntity<List<WatchlistItemResponse>> list(@AuthenticationPrincipal Jwt jwt) {
		Long userId = userIdFrom(jwt);
		return ResponseEntity.ok(watchlistService.listByUserId(userId));
	}

	@PostMapping
	public ResponseEntity<WatchlistItemResponse> add(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody AddWatchlistRequest request
	) {
		Long userId = userIdFrom(jwt);
		WatchlistItemResponse item = watchlistService.add(userId, request);
		return ResponseEntity.status(201).body(item);
	}

	@DeleteMapping
	public ResponseEntity<Void> remove(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam String termCode,
			@RequestParam String sisSectionId
	) {
		Long userId = userIdFrom(jwt);
		watchlistService.remove(userId, termCode, sisSectionId);
		return ResponseEntity.noContent().build();
	}

	private static Long userIdFrom(Jwt jwt) {
		if (jwt == null) throw new WatchlistException("Unauthorized");
		return Long.valueOf(jwt.getSubject());
	}
}
