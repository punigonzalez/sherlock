package com.sherlock.logdetective.detection.domain;

import java.util.Objects;

import com.sherlock.logdetective.ingestion.domain.EventLevel;

/**
 * Formal mapping from event level to incident severity.
 */
public final class SeverityClassifier {

	public Severity classify(EventLevel level) {
		Objects.requireNonNull(level, "level");
		return switch (level) {
			case ERROR -> Severity.HIGH;
			case WARN -> Severity.MEDIUM;
			case INFO, DEBUG, TRACE -> Severity.LOW;
			case UNKNOWN -> Severity.MEDIUM;
		};
	}

	public boolean isErrorSignal(EventLevel level) {
		return level == EventLevel.ERROR;
	}
}
