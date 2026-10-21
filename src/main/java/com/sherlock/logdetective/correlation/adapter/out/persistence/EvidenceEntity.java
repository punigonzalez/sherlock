package com.sherlock.logdetective.correlation.adapter.out.persistence;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "evidence")
public class EvidenceEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String kind;

	@Column(nullable = false)
	private String strength;

	@Column(nullable = false)
	private String reason;

	@Column(nullable = false)
	private String summary;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, String> attributes = new HashMap<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "evidence_event_links", joinColumns = @JoinColumn(name = "evidence_id"))
	@Column(name = "event_id")
	private Set<UUID> eventIds = new HashSet<>();

	protected EvidenceEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getStrength() {
		return strength;
	}

	public void setStrength(String strength) {
		this.strength = strength;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Set<UUID> getEventIds() {
		return eventIds;
	}

	public void setEventIds(Set<UUID> eventIds) {
		this.eventIds = eventIds;
	}
}
