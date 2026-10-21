package com.sherlock.logdetective.detection.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sherlock.logdetective.detection.domain.ErrorFingerprint;
import com.sherlock.logdetective.detection.domain.ErrorObservation;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.domain.IncidentStatus;
import com.sherlock.logdetective.detection.domain.Severity;
import com.sherlock.logdetective.detection.domain.fingerprint.ErrorFingerprintCalculator;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;
import com.sherlock.logdetective.ingestion.domain.EventLevel;
import com.sherlock.logdetective.shared.domain.PageResult;

class RecordErrorObservationUseCaseTest {

	@Test
	void groupsDuplicateObservationsIntoOneIncident() {
		InMemoryIncidentRepository repository = new InMemoryIncidentRepository();
		RecordErrorObservationUseCase useCase = new RecordErrorObservationUseCase(
				repository,
				new ErrorFingerprintCalculator()
		);

		Incident first = useCase.execute(observation("NullPointer at 550e8400-e29b-41d4-a716-446655440000"));
		Incident second = useCase.execute(observation("NullPointer at 123e4567-e89b-12d3-a456-426614174000"));

		assertThat(first.id()).isEqualTo(second.id());
		assertThat(second.occurrenceCount()).isEqualTo(2);
		assertThat(repository.size()).isEqualTo(1);
	}

	private static ErrorObservation observation(String message) {
		return new ErrorObservation(
				Instant.parse("2026-10-20T15:00:00Z"),
				EventLevel.ERROR,
				message,
				Optional.of("app.Main"),
				Optional.of("api"),
				Optional.empty(),
				Optional.empty(),
				Severity.HIGH,
				Map.of()
		);
	}

	private static final class InMemoryIncidentRepository implements IncidentRepository {

		private final Map<String, Incident> byFingerprint = new HashMap<>();
		private final Map<UUID, Incident> byId = new HashMap<>();

		@Override
		public Incident save(Incident incident) {
			byFingerprint.put(incident.fingerprint().value(), incident);
			byId.put(incident.id(), incident);
			return incident;
		}

		@Override
		public Optional<Incident> findById(UUID id) {
			return Optional.ofNullable(byId.get(id));
		}

		@Override
		public Optional<Incident> findByFingerprint(ErrorFingerprint fingerprint) {
			return Optional.ofNullable(byFingerprint.get(fingerprint.value()));
		}

		@Override
		public PageResult<Incident> findPage(int page, int size, Optional<IncidentStatus> status) {
			List<Incident> all = byId.values().stream()
					.filter(i -> status.isEmpty() || i.status() == status.get())
					.sorted((a, b) -> b.lastSeenAt().compareTo(a.lastSeenAt()))
					.toList();
			int from = Math.min(page * size, all.size());
			int to = Math.min(from + size, all.size());
			return PageResult.of(all.subList(from, to), page, size, all.size());
		}

		int size() {
			return byFingerprint.size();
		}
	}
}
