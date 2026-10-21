package com.sherlock.logdetective.correlation.domain;

import java.util.Objects;
import java.util.UUID;

public record EvidenceId(UUID value) {

	public EvidenceId {
		Objects.requireNonNull(value, "value");
	}

	public static EvidenceId generate() {
		return new EvidenceId(UUID.randomUUID());
	}
}
