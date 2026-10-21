package com.sherlock.logdetective.detection.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "incident_evidence_links")
public class IncidentEvidenceLinkEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "incident_id", nullable = false)
	private IncidentEntity incident;

	@Column(name = "evidence_ref", nullable = false)
	private String evidenceRef;

	@Column(name = "linked_at", nullable = false)
	private Instant linkedAt;

	protected IncidentEvidenceLinkEntity() {
	}

	public IncidentEvidenceLinkEntity(UUID id, IncidentEntity incident, String evidenceRef, Instant linkedAt) {
		this.id = id;
		this.incident = incident;
		this.evidenceRef = evidenceRef;
		this.linkedAt = linkedAt;
	}

	public String getEvidenceRef() {
		return evidenceRef;
	}
}
