package com.sherlock.logdetective.investigation.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Reduces prompt-injection risk from untrusted log text before it enters model prompts.
 * Does not claim to be complete; treats logs as data, never as instructions.
 */
public final class PromptSanitizer {

	private static final Pattern INSTRUCTION_MARKERS = Pattern.compile(
			"(?i)(ignore (all |previous )?instructions|system\\s*:|assistant\\s*:|you are now|\\bDAN\\b|<\\|im_start\\|>)"
	);

	private final int maxChars;

	public PromptSanitizer(int maxChars) {
		this.maxChars = Math.max(64, maxChars);
	}

	public String wrapUntrusted(String label, String raw) {
		Objects.requireNonNull(label, "label");
		String text = raw == null ? "" : raw;
		text = INSTRUCTION_MARKERS.matcher(text).replaceAll("[filtered]");
		if (text.length() > maxChars) {
			text = text.substring(0, maxChars) + "…[truncated]";
		}
		return "<UNTRUSTED_LOG source=\"" + label + "\">\n" + text + "\n</UNTRUSTED_LOG>";
	}
}
