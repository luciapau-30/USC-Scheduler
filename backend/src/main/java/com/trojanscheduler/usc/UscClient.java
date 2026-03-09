package com.trojanscheduler.usc;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Component
public class UscClient {

	private final WebClient webClient;
	private final int retryMaxAttempts;
	private final long retryInitialBackoffMs;
	private final long retryMaxBackoffMs;
	private final int rateLimitMaxRetries;
	private final int readTimeoutMs;

	public UscClient(
			WebClient.Builder builder,
			UscClientProperties properties
	) {
		this.retryMaxAttempts = properties.getRetryMaxAttempts();
		this.retryInitialBackoffMs = properties.getRetryInitialBackoffMs();
		this.retryMaxBackoffMs = properties.getRetryMaxBackoffMs();
		this.rateLimitMaxRetries = 2;
		this.readTimeoutMs = properties.getReadTimeoutMs();

		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMs())
				.responseTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
				.doOnConnected(conn -> conn
						.addHandlerLast(new ReadTimeoutHandler(properties.getReadTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS))
						.addHandlerLast(new WriteTimeoutHandler(properties.getReadTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS)));

		this.webClient = builder
				.baseUrl(properties.getBaseUrl().replaceAll("/$", ""))
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.defaultHeader(HttpHeaders.ACCEPT, "application/json")
				.defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
				.codecs(config -> config.defaultCodecs().maxInMemorySize(1024 * 1024))
				.build();
	}

	/**
	 * GET the path (relative to base URL) and return the response body as a string.
	 * Uses retries with backoff for 5xx and connection errors; respects 429 Retry-After when present.
	 */
	public String get(String path) {
		String normalizedPath = path.startsWith("/") ? path : "/" + path;
		Mono<String> mono = getInternal(normalizedPath);
		for (int i = 0; i < rateLimitMaxRetries; i++) {
			mono = mono.onErrorResume(UscException.class, e -> {
				if (!e.isRateLimited()) return Mono.error(e);
				int delay = e.getRetryAfterSeconds() != null ? e.getRetryAfterSeconds() : 60;
				return Mono.delay(Duration.ofSeconds(delay)).then(getInternal(normalizedPath));
			});
		}
		Mono<String> withTimeoutHandling = mono
				.retryWhen(Retry.backoff(retryMaxAttempts, Duration.ofMillis(retryInitialBackoffMs))
						.maxBackoff(Duration.ofMillis(retryMaxBackoffMs))
						.filter(t -> t instanceof UscException && ((UscException) t).isServerError()))
				.onErrorMap(t -> isTimeout(t), t -> new UscException("USC API timed out. Please try again in a moment.", 504, null));
		try {
			return withTimeoutHandling.block();
		} catch (Exception e) {
			if (isTimeout(e)) throw new UscException("USC API timed out. Please try again in a moment.", 504, null);
			if (isConnectionError(e)) throw new UscException("USC classes server is not reachable. Check your connection or try again in a few minutes.", 504, null);
			throw e;
		}
	}

	private static boolean isTimeout(Throwable t) {
		for (Throwable x = t; x != null; x = x.getCause()) {
			if (x instanceof TimeoutException) return true;
		}
		return false;
	}

	/** Connection refused, connect timeout, or other network failure talking to USC. */
	private static boolean isConnectionError(Throwable t) {
		for (Throwable x = t; x != null; x = x.getCause()) {
			if (x instanceof WebClientRequestException) return true;
			String name = x.getClass().getName();
			if (name.contains("ConnectTimeoutException") || name.contains("ConnectException")
					|| name.contains("UnknownHostException") || name.contains("IOException")) return true;
		}
		if (t != null && t.getMessage() != null) {
			String msg = t.getMessage();
			if (msg.contains("connection timed out") || msg.contains("Connection refused")) return true;
		}
		return false;
	}

	/**
	 * Reactive: GET the path and return the body. Caller can add retry or block.
	 */
	public Mono<String> getMono(String path) {
		String normalizedPath = path.startsWith("/") ? path : "/" + path;
		return getInternal(normalizedPath);
	}

	private Mono<String> getInternal(String path) {
		return webClient.get()
				.uri(path)
				.exchangeToMono(this::handleResponse)
				.timeout(Duration.ofMillis(readTimeoutMs));
	}

	private Mono<String> handleResponse(ClientResponse response) {
		HttpStatusCode status = response.statusCode();
		int code = status.value();

		if (response.statusCode().is2xxSuccessful()) {
			return response.bodyToMono(String.class);
		}

		if (code == 429) {
			List<String> retryAfter = response.headers().header("Retry-After");
			int delaySeconds = 60;
			if (!retryAfter.isEmpty()) {
				try {
					delaySeconds = Math.min(120, Math.max(1, Integer.parseInt(retryAfter.get(0).trim())));
				} catch (NumberFormatException ignored) {
					// use default
				}
			}
			return response.releaseBody()
					.then(Mono.<String>error(new UscException("USC API rate limited (429)", 429, delaySeconds)));
		}

		return response.bodyToMono(String.class)
				.defaultIfEmpty(status.toString())
				.flatMap(body -> Mono.<String>error(new UscException(
						"USC API error: " + code + " " + body, code, null)));
	}
}
