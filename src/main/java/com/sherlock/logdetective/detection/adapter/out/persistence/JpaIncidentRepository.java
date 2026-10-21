package com.sherlock.logdetective.detection.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sherlock.logdetective.detection.domain.ErrorFingerprint;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.domain.IncidentStatus;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;
import com.sherlock.logdetective.shared.domain.PageResult;

@Repository
@Transactional
public class JpaIncidentRepository implements IncidentRepository {

	private final SpringDataIncidentJpaRepository incidents;
	private final SpringDataFingerprintJpaRepository fingerprints;

	public JpaIncidentRepository(
			SpringDataIncidentJpaRepository incidents,
			SpringDataFingerprintJpaRepository fingerprints
	) {
		this.incidents = incidents;
		this.fingerprints = fingerprints;
	}

	@Override
	public Incident save(Incident incident) {
		ErrorFingerprintEntity fingerprintEntity = fingerprints.findById(incident.fingerprint().value())
				.orElseGet(() -> fingerprints.save(new ErrorFingerprintEntity(
						incident.fingerprint().value(),
						incident.fingerprint().normalizedSignature(),
						Instant.now()
				)));

		IncidentEntity entity = incidents.findById(incident.id()).orElseGet(IncidentEntity::new);
		IncidentMapper.applyDomain(incident, entity, fingerprintEntity);
		IncidentEntity saved = incidents.saveAndFlush(entity);
		// Touch collections inside the transaction for mapping.
		saved.getStatusHistory().size();
		saved.getEvidenceLinks().size();
		return IncidentMapper.toDomain(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Incident> findById(UUID id) {
		return incidents.findById(id).map(entity -> {
			entity.getStatusHistory().size();
			entity.getEvidenceLinks().size();
			return IncidentMapper.toDomain(entity);
		});
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Incident> findByFingerprint(ErrorFingerprint fingerprint) {
		return incidents.findByFingerprint_Fingerprint(fingerprint.value()).map(entity -> {
			entity.getStatusHistory().size();
			entity.getEvidenceLinks().size();
			return IncidentMapper.toDomain(entity);
		});
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<Incident> findPage(int page, int size, Optional<IncidentStatus> status) {
		PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastSeenAt"));
		Page<IncidentEntity> result = status
				.map(s -> incidents.findByStatus(s.name(), pageable))
				.orElseGet(() -> incidents.findAll(pageable));
		var items = result.getContent().stream().map(entity -> {
			entity.getStatusHistory().size();
			entity.getEvidenceLinks().size();
			return IncidentMapper.toDomain(entity);
		}).toList();
		return PageResult.of(items, page, size, result.getTotalElements());
	}
}
