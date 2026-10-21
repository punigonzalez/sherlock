package com.sherlock.logdetective.shared.domain;

import java.time.Instant;
import java.util.Objects;

public record TimeRange(Instant from, Instant to) {

	public TimeRange {
		Objects.requireNonNull(from, "from");
		Objects.requireNonNull(to, "to");
		if (to.isBefore(from)) {
			throw new IllegalArgumentException("to must not be before from");
		}
	}
}
