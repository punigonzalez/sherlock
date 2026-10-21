package com.sherlock.logdetective.ingestion.adapter.out.persistence;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "canonical_events")
public class CanonicalEventEntity {

	@Id
	private UUID id;

	@Column(name = "source_id", nullable = false)
	private String sourceId;

	@Column(name = "source_payload_id", nullable = false)
	private String sourcePayloadId;

	@Column(name = "event_timestamp", nullable = false)
	private Instant eventTimestamp;

	@Column(name = "ingested_at", nullable = false)
	private Instant ingestedAt;

	@Column(name = "level", nullable = false)
	private String level;

	@Column(name = "message", nullable = false)
	private String message;

	@Column(name = "logger")
	private String logger;

	@Column(name = "service")
	private String service;

	@Column(name = "host")
	private String host;

	@Column(name = "trace_id")
	private String traceId;

	@Column(name = "span_id")
	private String spanId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
	private Map<String, String> attributes = new HashMap<>();

	protected CanonicalEventEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getSourcePayloadId() {
		return sourcePayloadId;
	}

	public void setSourcePayloadId(String sourcePayloadId) {
		this.sourcePayloadId = sourcePayloadId;
	}

	public Instant getEventTimestamp() {
		return eventTimestamp;
	}

	public void setEventTimestamp(Instant eventTimestamp) {
		this.eventTimestamp = eventTimestamp;
	}

	public Instant getIngestedAt() {
		return ingestedAt;
	}

	public void setIngestedAt(Instant ingestedAt) {
		this.ingestedAt = ingestedAt;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getLogger() {
		return logger;
	}

	public void setLogger(String logger) {
		this.logger = logger;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public String getSpanId() {
		return spanId;
	}

	public void setSpanId(String spanId) {
		this.spanId = spanId;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
}
