package com.trojanscheduler.usc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.usc")
public class UscClientProperties {

	private String baseUrl = "https://classes.usc.edu";
	private int connectTimeoutMs = 5000;
	private int readTimeoutMs = 15000;
	private String userAgent = "TrojanScheduler/1.0";
	private int retryMaxAttempts = 3;
	private long retryInitialBackoffMs = 1000;
	private long retryMaxBackoffMs = 30000;
	private String searchPathTemplate = "/api/Search/Basic?termCode={termCode}&searchTerm={q}";
	private String sectionPathTemplate = "/api/Search/Basic?termCode={termCode}&searchTerm={prefix}";

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public int getConnectTimeoutMs() {
		return connectTimeoutMs;
	}

	public void setConnectTimeoutMs(int connectTimeoutMs) {
		this.connectTimeoutMs = connectTimeoutMs;
	}

	public int getReadTimeoutMs() {
		return readTimeoutMs;
	}

	public void setReadTimeoutMs(int readTimeoutMs) {
		this.readTimeoutMs = readTimeoutMs;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public int getRetryMaxAttempts() {
		return retryMaxAttempts;
	}

	public void setRetryMaxAttempts(int retryMaxAttempts) {
		this.retryMaxAttempts = retryMaxAttempts;
	}

	public long getRetryInitialBackoffMs() {
		return retryInitialBackoffMs;
	}

	public void setRetryInitialBackoffMs(long retryInitialBackoffMs) {
		this.retryInitialBackoffMs = retryInitialBackoffMs;
	}

	public long getRetryMaxBackoffMs() {
		return retryMaxBackoffMs;
	}

	public void setRetryMaxBackoffMs(long retryMaxBackoffMs) {
		this.retryMaxBackoffMs = retryMaxBackoffMs;
	}

	public String getSearchPathTemplate() {
		return searchPathTemplate;
	}

	public void setSearchPathTemplate(String searchPathTemplate) {
		this.searchPathTemplate = searchPathTemplate;
	}

	public String getSectionPathTemplate() {
		return sectionPathTemplate;
	}

	public void setSectionPathTemplate(String sectionPathTemplate) {
		this.sectionPathTemplate = sectionPathTemplate;
	}
}
