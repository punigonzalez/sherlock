package com.sherlock.logdetective.correlation.domain;

/**
 * How strongly evidence supports a relationship. Never implies unsupported causality.
 */
public enum CorrelationStrength {
	/** Shared trace ID (or equally hard link). */
	CONFIRMED,
	/** Multiple reinforcing signals without a hard shared key. */
	STRONG,
	/** Temporal/spatial proximity only. */
	WEAK,
	NONE
}
