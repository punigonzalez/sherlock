package com.sherlock.logdetective.detection.adapter.out.persistence;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "incidents")
public class IncidentEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false, cascade = CascadeType.PERSIST)
	@JoinColumn(name = "fingerprint", nullable = false)
	private ErrorFingerprintEntity fingerprint;

	@Column(name = "status", nullable = false)
	private String status;

	@Column(name = "severity", nullable = false)
	private String severity;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "first_seen_at", nullable = false)
	private Instant firstSeenAt;

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt;

	@Column(name = "occurrence_count", nullable = false)
	private long occurrenceCount;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
	private Map<String, String> metadata = new HashMap<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("changedAt ASC")
	private Set<IncidentStatusHistoryEntity> statusHistory = new HashSet<>();

	@OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IncidentEvidenceLinkEntity> evidenceLinks = new HashSet<>();

	protected IncidentEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public ErrorFingerprintEntity getFingerprint() {
		return fingerprint;
	}

	public void setFingerprint(ErrorFingerprintEntity fingerprint) {
		this.fingerprint = fingerprint;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Instant getFirstSeenAt() {
		return firstSeenAt;
	}

	public void setFirstSeenAt(Instant firstSeenAt) {
		this.firstSeenAt = firstSeenAt;
	}

	public Instant getLastSeenAt() {
		return lastSeenAt;
	}

	public void setLastSeenAt(Instant lastSeenAt) {
		this.lastSeenAt = lastSeenAt;
	}

	public long getOccurrenceCount() {
		return occurrenceCount;
	}

	public void setOccurrenceCount(long occurrenceCount) {
		this.occurrenceCount = occurrenceCount;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Set<IncidentStatusHistoryEntity> getStatusHistory() {
		return statusHistory;
	}

	public Set<IncidentEvidenceLinkEntity> getEvidenceLinks() {
		return evidenceLinks;
	}
}
