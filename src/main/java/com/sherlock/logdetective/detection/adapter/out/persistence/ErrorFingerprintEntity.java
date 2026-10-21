package com.sherlock.logdetective.detection.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "error_fingerprints")
public class ErrorFingerprintEntity {

	@Id
	@Column(name = "fingerprint", nullable = false, updatable = false)
	private String fingerprint;

	@Column(name = "normalized_signature", nullable = false)
	private String normalizedSignature;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ErrorFingerprintEntity() {
	}

	public ErrorFingerprintEntity(String fingerprint, String normalizedSignature, Instant createdAt) {
		this.fingerprint = fingerprint;
		this.normalizedSignature = normalizedSignature;
		this.createdAt = createdAt;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	public String getNormalizedSignature() {
		return normalizedSignature;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
