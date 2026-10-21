package com.sherlock.logdetective.detection.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "incident_status_history")
public class IncidentStatusHistoryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "incident_id", nullable = false)
	private IncidentEntity incident;

	@Column(name = "from_status")
	private String fromStatus;

	@Column(name = "to_status", nullable = false)
	private String toStatus;

	@Column(name = "changed_at", nullable = false)
	private Instant changedAt;

	@Column(name = "reason", nullable = false)
	private String reason;

	protected IncidentStatusHistoryEntity() {
	}

	public IncidentStatusHistoryEntity(
			IncidentEntity incident,
			String fromStatus,
			String toStatus,
			Instant changedAt,
			String reason
	) {
		this.incident = incident;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.changedAt = changedAt;
		this.reason = reason;
	}

	public String getFromStatus() {
		return fromStatus;
	}

	public String getToStatus() {
		return toStatus;
	}

	public Instant getChangedAt() {
		return changedAt;
	}

	public String getReason() {
		return reason;
	}
}
