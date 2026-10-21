package com.sherlock.logdetective.ingestion.domain.redaction;

/**
 * Minimal redaction applied after normalize / before persist.
 */
public interface RedactionHook {

	String redact(String message);

	RedactionHook NO_OP = message -> message;
}
