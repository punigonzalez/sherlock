package com.sherlock.logdetective.cli;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;

import com.sherlock.logdetective.correlation.application.QueryEvidenceUseCase;
import com.sherlock.logdetective.correlation.domain.Evidence;
import com.sherlock.logdetective.detection.application.QueryIncidentsUseCase;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.domain.IncidentStatus;
import com.sherlock.logdetective.ingestion.application.IngestBatchResult;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase.SourceView;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase.ValidationResult;
import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase;
import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase.InvestigationView;
import com.sherlock.logdetective.shared.domain.PageResult;

/**
 * Lightweight CLI over application use cases (no persistence adapters).
 */
public final class CliFacade {

	private final ManageFileSourcesUseCase manageFileSourcesUseCase;
	private final QueryIncidentsUseCase queryIncidentsUseCase;
	private final QueryEvidenceUseCase queryEvidenceUseCase;
	private final RequestInvestigationUseCase requestInvestigationUseCase;

	public CliFacade(
			ManageFileSourcesUseCase manageFileSourcesUseCase,
			QueryIncidentsUseCase queryIncidentsUseCase,
			QueryEvidenceUseCase queryEvidenceUseCase,
			RequestInvestigationUseCase requestInvestigationUseCase
	) {
		this.manageFileSourcesUseCase = manageFileSourcesUseCase;
		this.queryIncidentsUseCase = queryIncidentsUseCase;
		this.queryEvidenceUseCase = queryEvidenceUseCase;
		this.requestInvestigationUseCase = requestInvestigationUseCase;
	}

	public int execute(ApplicationArguments args, PrintStream out, PrintStream err) {
		List<String> commands = args.getOptionValues("cli");
		if (commands == null || commands.isEmpty() || commands.getFirst().isBlank()) {
			printUsage(err);
			return 2;
		}
		String command = commands.getFirst().trim();
		try {
			return switch (command) {
				case "help" -> {
					printUsage(out);
					yield 0;
				}
				case "validate-source" -> validateSource(args, out);
				case "register-source" -> registerSource(args, out);
				case "list-sources" -> listSources(out);
				case "source-health" -> sourceHealth(args, out, err);
				case "ingest" -> ingest(args, out);
				case "list-incidents" -> listIncidents(args, out);
				case "show-incident" -> showIncident(args, out, err);
				case "list-evidence" -> listEvidence(args, out, err);
				case "request-investigation" -> requestInvestigation(args, out, err);
				case "show-investigation" -> showInvestigation(args, out, err);
				default -> {
					err.println("Unknown CLI command: " + command);
					printUsage(err);
					yield 2;
				}
			};
		}
		catch (IllegalArgumentException | IllegalStateException ex) {
			err.println("Error: " + ex.getMessage());
			return 1;
		}
	}

	private int validateSource(ApplicationArguments args, PrintStream out) {
		String id = requireOption(args, "id");
		String path = requireOption(args, "path");
		String encoding = optional(args, "encoding").orElse("UTF-8");
		ValidationResult result = manageFileSourcesUseCase.validate(id, path, encoding);
		out.printf(
				"valid=%s id=%s path=%s status=%s detail=%s%n",
				result.valid(),
				result.sourceId(),
				result.resolvedPath(),
				result.status(),
				result.detail() == null ? "" : result.detail()
		);
		if (result.errorMessage() != null) {
			out.println("error=" + result.errorMessage());
		}
		return result.valid() ? 0 : 1;
	}

	private int registerSource(ApplicationArguments args, PrintStream out) {
		String id = requireOption(args, "id");
		String path = requireOption(args, "path");
		String encoding = optional(args, "encoding").orElse("UTF-8");
		SourceView view = manageFileSourcesUseCase.registerAndStart(id, path, encoding);
		printSource(out, view);
		return 0;
	}

	private int listSources(PrintStream out) {
		List<SourceView> sources = manageFileSourcesUseCase.list();
		if (sources.isEmpty()) {
			out.println("(no running sources)");
			return 0;
		}
		sources.forEach(view -> printSource(out, view));
		return 0;
	}

	private int sourceHealth(ApplicationArguments args, PrintStream out, PrintStream err) {
		String id = requireOption(args, "id");
		Optional<SourceView> view = manageFileSourcesUseCase.health(id);
		if (view.isEmpty()) {
			err.println("source not found: " + id);
			return 1;
		}
		printSource(out, view.get());
		return 0;
	}

	private int ingest(ApplicationArguments args, PrintStream out) {
		String id = requireOption(args, "id");
		IngestBatchResult result = manageFileSourcesUseCase.ingestOnce(id);
		out.printf(
				"fetched=%d accepted=%d duplicates=%d parseFailures=%d errorsRouted=%d%n",
				result.fetched(),
				result.accepted(),
				result.duplicates(),
				result.parseFailures(),
				result.errorsRoutedToIncidents()
		);
		return 0;
	}

	private int listIncidents(ApplicationArguments args, PrintStream out) {
		int page = optional(args, "page").map(Integer::parseInt).orElse(0);
		int size = optional(args, "size").map(Integer::parseInt).orElse(20);
		Optional<IncidentStatus> status = optional(args, "status")
				.map(s -> IncidentStatus.valueOf(s.trim().toUpperCase()));
		PageResult<Incident> result = queryIncidentsUseCase.list(page, size, status);
		out.printf(
				"page=%d size=%d total=%d totalPages=%d%n",
				result.page(),
				result.size(),
				result.totalElements(),
				result.totalPages()
		);
		for (Incident incident : result.items()) {
			out.printf(
					"%s status=%s severity=%s count=%d title=%s%n",
					incident.id(),
					incident.status(),
					incident.severity(),
					incident.occurrenceCount(),
					incident.title()
			);
		}
		return 0;
	}

	private int showIncident(ApplicationArguments args, PrintStream out, PrintStream err) {
		UUID id = UUID.fromString(requireOption(args, "id"));
		Optional<Incident> incident = queryIncidentsUseCase.get(id);
		if (incident.isEmpty()) {
			err.println("incident not found: " + id);
			return 1;
		}
		Incident i = incident.get();
		out.printf(
				"id=%s status=%s severity=%s fingerprint=%s count=%d first=%s last=%s%n",
				i.id(),
				i.status(),
				i.severity(),
				i.fingerprint().value(),
				i.occurrenceCount(),
				i.firstSeenAt(),
				i.lastSeenAt()
		);
		out.println("title=" + i.title());
		out.println("evidenceRefs=" + i.evidenceRefs());
		return 0;
	}

	private int listEvidence(ApplicationArguments args, PrintStream out, PrintStream err) {
		UUID id = UUID.fromString(requireOption(args, "id"));
		if (queryIncidentsUseCase.get(id).isEmpty()) {
			err.println("incident not found: " + id);
			return 1;
		}
		List<Evidence> evidence = queryEvidenceUseCase.forIncident(id);
		if (evidence.isEmpty()) {
			out.println("(no evidence)");
			return 0;
		}
		for (Evidence e : evidence) {
			out.printf(
					"%s kind=%s strength=%s reason=%s summary=%s%n",
					e.id().value(),
					e.kind(),
					e.strength(),
					e.reason(),
					e.summary()
			);
		}
		return 0;
	}

	private int requestInvestigation(ApplicationArguments args, PrintStream out, PrintStream err) {
		UUID id = UUID.fromString(requireOption(args, "id"));
		Optional<InvestigationView> view = requestInvestigationUseCase.request(id);
		if (view.isEmpty()) {
			err.println("incident not found: " + id);
			return 1;
		}
		printInvestigation(out, view.get());
		return 0;
	}

	private int showInvestigation(ApplicationArguments args, PrintStream out, PrintStream err) {
		UUID id = UUID.fromString(requireOption(args, "id"));
		Optional<InvestigationView> view = requestInvestigationUseCase.latest(id);
		if (view.isEmpty()) {
			err.println("no investigation report available for incident: " + id);
			return 1;
		}
		printInvestigation(out, view.get());
		return 0;
	}

	private static void printInvestigation(PrintStream out, InvestigationView view) {
		out.printf(
				"incident=%s status=%s aiUnavailable=%s createdAt=%s%n",
				view.incidentId(),
				view.status(),
				view.aiUnavailable(),
				view.createdAt()
		);
		out.println("summary=" + view.summary());
		out.println("evidenceCitations=" + view.evidenceCitationIds());
		if (view.aiUnavailable()) {
			out.println("note=AI investigator is NoOp; this is not a real diagnosis.");
		}
	}

	private static void printSource(PrintStream out, SourceView view) {
		out.printf(
				"id=%s type=%s path=%s running=%s health=%s detail=%s%n",
				view.id(),
				view.type(),
				view.path(),
				view.running(),
				view.healthStatus(),
				view.healthDetail() == null ? "" : view.healthDetail()
		);
	}

	private static String requireOption(ApplicationArguments args, String name) {
		return optional(args, name)
				.orElseThrow(() -> new IllegalArgumentException("missing required --" + name));
	}

	private static Optional<String> optional(ApplicationArguments args, String name) {
		List<String> values = args.getOptionValues(name);
		if (values == null || values.isEmpty() || values.getFirst() == null || values.getFirst().isBlank()) {
			return Optional.empty();
		}
		return Optional.of(values.getFirst());
	}

	static void printUsage(PrintStream out) {
		out.println("""
				Log Detective CLI (local MVP). Pass --cli=<command>.

				Commands:
				  help
				  validate-source --id=<id> --path=<path> [--encoding=UTF-8]
				  register-source --id=<id> --path=<path> [--encoding=UTF-8]
				  list-sources
				  source-health --id=<id>
				  ingest --id=<id>
				  list-incidents [--page=0] [--size=20] [--status=OPEN]
				  show-incident --id=<uuid>
				  list-evidence --id=<incident-uuid>
				  request-investigation --id=<incident-uuid>
				  show-investigation --id=<incident-uuid>

				Notes:
				  - Paths must resolve under logdetective.ingestion.allowlist-base-dir
				  - Investigation uses NoOp AI until Phase 6 (honest unavailable status)
				  - Actuator health: GET /actuator/health (server binds 127.0.0.1 by default)
				""");
	}
}
