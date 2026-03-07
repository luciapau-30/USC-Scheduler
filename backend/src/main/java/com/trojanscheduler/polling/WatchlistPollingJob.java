package com.trojanscheduler.polling;

import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs watchlist-driven section polling on a fixed interval with random jitter
 * to avoid thundering herd when multiple instances run.
 */
@Component
public class WatchlistPollingJob {

	private static final Logger log = LoggerFactory.getLogger(WatchlistPollingJob.class);

	private final WatchlistPollingService pollingService;
	private final long jitterMs;

	public WatchlistPollingJob(
			WatchlistPollingService pollingService,
			@Value("${app.polling.jitter-ms:60000}") long jitterMs
	) {
		this.pollingService = pollingService;
		this.jitterMs = jitterMs;
	}

	@Scheduled(fixedDelayString = "${app.polling.interval-ms:600000}", initialDelayString = "${app.polling.initial-delay-ms:30000}")
	public void run() {
		if (jitterMs > 0) {
			long sleep = ThreadLocalRandom.current().nextLong(0, jitterMs + 1);
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.warn("Poll jitter sleep interrupted");
				return;
			}
		}
		log.trace("Starting watchlist poll cycle");
		pollingService.runPollCycle();
	}
}
