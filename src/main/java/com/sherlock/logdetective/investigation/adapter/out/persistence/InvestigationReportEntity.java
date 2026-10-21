package com.sherlock.logdetective.investigation.adapter.out.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "investigation_reports")
public class InvestigationReportEntity {

	@Id
	private UUID id;

	@Column(name = "incident_id", nullable = false)
	private UUID incidentId;

	@Column(nullable = false)
	private String status;

	@Column(nullable = false)
	private String provider;

	@Column(nullable = false, columnDefinition = "text")
	private String summary;

	@Column(columnDefinition = "text")
	private String hypothesis;

	@Column(nullable = false)
	private String confidence;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "evidence_citation_ids", nullable = false, columnDefinition = "jsonb")
	private List<String> evidenceCitationIds = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "tool_calls", nullable = false, columnDefinition = "jsonb")
	private List<Map<String, String>> toolCalls = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, String> metadata = new HashMap<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getIncidentId() {
		return incidentId;
	}

	public void setIncidentId(UUID incidentId) {
		this.incidentId = incidentId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getHypothesis() {
		return hypothesis;
	}

	public void setHypothesis(String hypothesis) {
		this.hypothesis = hypothesis;
	}

	public String getConfidence() {
		return confidence;
	}

	public void setConfidence(String confidence) {
		this.confidence = confidence;
	}

	public List<String> getEvidenceCitationIds() {
		return evidenceCitationIds;
	}

	public void setEvidenceCitationIds(List<String> evidenceCitationIds) {
		this.evidenceCitationIds = evidenceCitationIds;
	}

	public List<Map<String, String>> getToolCalls() {
		return toolCalls;
	}

	public void setToolCalls(List<Map<String, String>> toolCalls) {
		this.toolCalls = toolCalls;
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
}
