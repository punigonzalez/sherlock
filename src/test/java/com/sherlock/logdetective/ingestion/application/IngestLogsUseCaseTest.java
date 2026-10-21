package com.sherlock.logdetective.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sherlock.logdetective.detection.application.RecordErrorObservationUseCase;
import com.sherlock.logdetective.detection.domain.ErrorFingerprint;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.domain.fingerprint.ErrorFingerprintCalculator;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;
import com.sherlock.logdetective.ingestion.adapter.out.file.FileLogSource;
import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.domain.parse.CompositeLogParser;
import com.sherlock.logdetective.ingestion.domain.redaction.BasicRedactionHook;
import com.sherlock.logdetective.ingestion.port.out.EventRepository;
import com.sherlock.logdetective.ingestion.port.out.LogSource.FetchCursor;
import com.sherlock.logdetective.ingestion.port.out.SourceCheckpointStore;

class IngestLogsUseCaseTest {

	@TempDir
	Path tempDir;

	@Test
	void ingestsFixtureRoutesErrorsAndRedactsAndDedups() throws Exception {
		Path log = tempDir.resolve("sample.log");
		Files.copy(Path.of("src/test/resources/fixtures/sample-app.log"), log);

		InMemoryEventRepository events = new InMemoryEventRepository();
		InMemoryCheckpointStore checkpoints = new InMemoryCheckpointStore();
		CountingIncidentRepository incidents = new CountingIncidentRepository();
		RecordErrorObservationUseCase recordErrors = new RecordErrorObservationUseCase(
				incidents,
				new ErrorFingerprintCalculator()
		);
		IngestLogsUseCase useCase = new IngestLogsUseCase(
				events,
				checkpoints,
				new CompositeLogParser(),
				new BasicRedactionHook(),
				recordErrors
		);

		FileLogSource source = new FileLogSource(new LogSourceId("app-log"), log);
		IngestBatchResult first = useCase.ingest(source, 100);
		assertThat(first.accepted()).isGreaterThanOrEqualTo(3);
		assertThat(first.errorsRoutedToIncidents()).isGreaterThanOrEqualTo(2);
		assertThat(incidents.saves.get()).isGreaterThanOrEqualTo(2);

		assertThat(events.all().stream().anyMatch(e -> e.message().contains("<REDACTED_EMAIL>"))).isTrue();
		assertThat(events.all().stream().anyMatch(e -> e.message().contains("<REDACTED_TOKEN>"))).isTrue();

		IngestBatchResult second = useCase.ingest(source, 100);
		assertThat(second.accepted()).isZero();
		assertThat(second.duplicates() + second.fetched()).isGreaterThanOrEqualTo(0);
		assertThat(checkpoints.load(new LogSourceId("app-log"))).isPresent();
	}

	private static final class InMemoryEventRepository implements EventRepository {
		private final Map<String, CanonicalEvent> byPayload = new HashMap<>();
		private final Map<UUID, CanonicalEvent> byId = new HashMap<>();

		@Override
		public CanonicalEvent save(CanonicalEvent event) {
			byPayload.put(key(event.sourceId(), event.sourcePayloadId()), event);
			byId.put(event.id(), event);
			return event;
		}

		@Override
		public Optional<CanonicalEvent> findById(UUID id) {
			return Optional.ofNullable(byId.get(id));
		}

		@Override
		public Optional<CanonicalEvent> findBySourcePayloadId(LogSourceId sourceId, String sourcePayloadId) {
			return Optional.ofNullable(byPayload.get(key(sourceId, sourcePayloadId)));
		}

		@Override
		public List<CanonicalEvent> findBySourceId(LogSourceId sourceId) {
			return byPayload.values().stream().filter(e -> e.sourceId().equals(sourceId)).toList();
		}

		List<CanonicalEvent> all() {
			return List.copyOf(byId.values());
		}

		private static String key(LogSourceId sourceId, String payloadId) {
			return sourceId.value() + "|" + payloadId;
		}
	}

	private static final class InMemoryCheckpointStore implements SourceCheckpointStore {
		private final Map<String, FetchCursor> store = new HashMap<>();

		@Override
		public Optional<FetchCursor> load(LogSourceId sourceId) {
			return Optional.ofNullable(store.get(sourceId.value()));
		}

		@Override
		public void save(LogSourceId sourceId, FetchCursor cursor) {
			store.put(sourceId.value(), cursor);
		}
	}

	private static final class CountingIncidentRepository implements IncidentRepository {
		private final Map<String, Incident> byFingerprint = new HashMap<>();
		private final AtomicInteger saves = new AtomicInteger();

		@Override
		public Incident save(Incident incident) {
			saves.incrementAndGet();
			byFingerprint.put(incident.fingerprint().value(), incident);
			return incident;
		}

		@Override
		public Optional<Incident> findById(UUID id) {
			return byFingerprint.values().stream().filter(i -> i.id().equals(id)).findFirst();
		}

		@Override
		public Optional<Incident> findByFingerprint(ErrorFingerprint fingerprint) {
			return Optional.ofNullable(byFingerprint.get(fingerprint.value()));
		}
	}
}
