package com.sherlock.logdetective.detection.domain;

/**
 * Incident lifecycle states.
 *
 * <pre>
 *   OPEN ──────────────► INVESTIGATING ──────────────► RESOLVED
 *    ▲                        │                           │
 *    │                        ▼                           │
 *    └────────────── REOPENED ◄───────────────────────────┘
 * </pre>
 *
 * Allowed transitions (see {@link Incident#transitionTo}):
 * <ul>
 *   <li>OPEN → INVESTIGATING, RESOLVED</li>
 *   <li>INVESTIGATING → OPEN, RESOLVED</li>
 *   <li>RESOLVED → REOPENED (typically via a new observation)</li>
 *   <li>REOPENED → INVESTIGATING, RESOLVED, OPEN</li>
 * </ul>
 */
public enum IncidentStatus {
	OPEN,
	INVESTIGATING,
	RESOLVED,
	REOPENED
}
