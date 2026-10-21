package com.sherlock.logdetective.detection.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Incident aggregate grouped by {@link ErrorFingerprint}.
 */
public final class Incident {

	private final UUID id;
	private final ErrorFingerprint fingerprint;
	private IncidentStatus status;
	private Severity severity;
	private String title;
	private Instant firstSeenAt;
	private Instant lastSeenAt;
	private long occurrenceCount;
	private final Map<String, String> metadata;
	private final Set<String> evidenceRefs;
	private final List<StatusTransition> history;

	private Incident(
			UUID id,
			ErrorFingerprint fingerprint,
			IncidentStatus status,
			Severity severity,
			String title,
			Instant firstSeenAt,
			Instant lastSeenAt,
			long occurrenceCount,
			Map<String, String> metadata,
			Set<String> evidenceRefs,
			List<StatusTransition> history
	) {
		this.id = Objects.requireNonNull(id, "id");
		this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
		this.status = Objects.requireNonNull(status, "status");
		this.severity = Objects.requireNonNull(severity, "severity");
		this.title = Objects.requireNonNull(title, "title");
		this.firstSeenAt = Objects.requireNonNull(firstSeenAt, "firstSeenAt");
		this.lastSeenAt = Objects.requireNonNull(lastSeenAt, "lastSeenAt");
		if (occurrenceCount < 1) {
			throw new IllegalArgumentException("occurrenceCount must be >= 1");
		}
		this.occurrenceCount = occurrenceCount;
		this.metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
		this.evidenceRefs = new LinkedHashSet<>(Objects.requireNonNull(evidenceRefs, "evidenceRefs"));
		this.history = new ArrayList<>(Objects.requireNonNull(history, "history"));
	}

	public static Incident openNew(
			ErrorFingerprint fingerprint,
			ErrorObservation observation,
			String title
	) {
		Instant at = observation.observedAt();
		Incident incident = new Incident(
				UUID.randomUUID(),
				fingerprint,
				IncidentStatus.OPEN,
				observation.severity(),
				title,
				at,
				at,
				1L,
				observation.metadata(),
				new LinkedHashSet<>(),
				new ArrayList<>()
		);
		incident.history.add(new StatusTransition(null, IncidentStatus.OPEN, at, "created"));
		observation.evidenceRef().ifPresent(incident.evidenceRefs::add);
		return incident;
	}

	public static Incident rehydrate(
			UUID id,
			ErrorFingerprint fingerprint,
			IncidentStatus status,
			Severity severity,
			String title,
			Instant firstSeenAt,
			Instant lastSeenAt,
			long occurrenceCount,
			Map<String, String> metadata,
			Set<String> evidenceRefs,
			List<StatusTransition> history
	) {
		return new Incident(
				id,
				fingerprint,
				status,
				severity,
				title,
				firstSeenAt,
				lastSeenAt,
				occurrenceCount,
				metadata,
				evidenceRefs,
				history
		);
	}

	public void applyObservation(ErrorObservation observation) {
		Objects.requireNonNull(observation, "observation");
		if (observation.observedAt().isAfter(lastSeenAt)) {
			lastSeenAt = observation.observedAt();
		}
		occurrenceCount += 1;
		if (observation.severity().ordinal() > severity.ordinal()) {
			severity = observation.severity();
		}
		observation.evidenceRef().ifPresent(evidenceRefs::add);
		if (status == IncidentStatus.RESOLVED) {
			transitionTo(IncidentStatus.REOPENED, observation.observedAt(), "new observation");
		}
	}

	public void transitionTo(IncidentStatus target, Instant at, String reason) {
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(at, "at");
		if (status == target) {
			return;
		}
		if (!isAllowed(status, target)) {
			throw new IllegalStateException("Cannot transition from " + status + " to " + target);
		}
		IncidentStatus from = status;
		status = target;
		history.add(new StatusTransition(from, target, at, reason == null ? "" : reason));
	}

	static boolean isAllowed(IncidentStatus from, IncidentStatus to) {
		return switch (from) {
			case OPEN -> to == IncidentStatus.INVESTIGATING || to == IncidentStatus.RESOLVED;
			case INVESTIGATING -> to == IncidentStatus.OPEN || to == IncidentStatus.RESOLVED;
			case RESOLVED -> to == IncidentStatus.REOPENED;
			case REOPENED -> to == IncidentStatus.INVESTIGATING
					|| to == IncidentStatus.RESOLVED
					|| to == IncidentStatus.OPEN;
		};
	}

	public UUID id() {
		return id;
	}

	public ErrorFingerprint fingerprint() {
		return fingerprint;
	}

	public IncidentStatus status() {
		return status;
	}

	public Severity severity() {
		return severity;
	}

	public String title() {
		return title;
	}

	public Instant firstSeenAt() {
		return firstSeenAt;
	}

	public Instant lastSeenAt() {
		return lastSeenAt;
	}

	public long occurrenceCount() {
		return occurrenceCount;
	}

	public Map<String, String> metadata() {
		return metadata;
	}

	public Set<String> evidenceRefs() {
		return Set.copyOf(evidenceRefs);
	}

	public List<StatusTransition> history() {
		return List.copyOf(history);
	}

	public record StatusTransition(
			IncidentStatus from,
			IncidentStatus to,
			Instant at,
			String reason
	) {
		public StatusTransition {
			Objects.requireNonNull(to, "to");
			Objects.requireNonNull(at, "at");
			reason = reason == null ? "" : reason;
		}
	}
}
