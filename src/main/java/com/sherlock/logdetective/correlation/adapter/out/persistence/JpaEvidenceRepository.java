package com.sherlock.logdetective.correlation.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sherlock.logdetective.correlation.domain.CorrelationStrength;
import com.sherlock.logdetective.correlation.domain.Evidence;
import com.sherlock.logdetective.correlation.domain.EvidenceId;
import com.sherlock.logdetective.correlation.domain.EvidenceKind;
import com.sherlock.logdetective.correlation.port.out.EvidenceRepository;

@Repository
@Transactional
public class JpaEvidenceRepository implements EvidenceRepository {

	private final SpringDataEvidenceJpaRepository evidence;
	private final JdbcTemplate jdbcTemplate;

	public JpaEvidenceRepository(SpringDataEvidenceJpaRepository evidence, JdbcTemplate jdbcTemplate) {
		this.evidence = evidence;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Evidence save(Evidence domain) {
		EvidenceEntity entity = evidence.findById(domain.id().value()).orElseGet(EvidenceEntity::new);
		entity.setId(domain.id().value());
		entity.setKind(domain.kind().name());
		entity.setStrength(domain.strength().name());
		entity.setReason(domain.reason());
		entity.setSummary(domain.summary());
		entity.setAttributes(Map.copyOf(domain.attributes()));
		entity.setCreatedAt(domain.createdAt());
		entity.setEventIds(new HashSet<>(domain.eventIds()));
		EvidenceEntity saved = evidence.saveAndFlush(entity);
		return toDomain(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Evidence> findById(EvidenceId id) {
		return evidence.findById(id.value()).map(JpaEvidenceRepository::toDomain);
	}

	@Override
	public void linkToIncident(UUID incidentId, Evidence domainEvidence) {
		Integer exists = jdbcTemplate.queryForObject(
				"""
						SELECT COUNT(*) FROM incident_evidence_links
						WHERE incident_id = ? AND evidence_ref = ?
						""",
				Integer.class,
				incidentId,
				domainEvidence.id().value().toString()
		);
		if (exists != null && exists > 0) {
			return;
		}
		jdbcTemplate.update(
				"""
						INSERT INTO incident_evidence_links (id, incident_id, evidence_ref, linked_at, strength, reason)
						VALUES (?, ?, ?, ?, ?, ?)
						""",
				UUID.randomUUID(),
				incidentId,
				domainEvidence.id().value().toString(),
				Timestamp.from(Instant.now()),
				domainEvidence.strength().name(),
				domainEvidence.reason()
		);
	}

	private static Evidence toDomain(EvidenceEntity entity) {
		return new Evidence(
				new EvidenceId(entity.getId()),
				EvidenceKind.valueOf(entity.getKind()),
				CorrelationStrength.valueOf(entity.getStrength()),
				entity.getReason(),
				entity.getSummary(),
				entity.getEventIds() == null ? java.util.Set.of() : entity.getEventIds(),
				entity.getAttributes() == null ? Map.of() : entity.getAttributes(),
				entity.getCreatedAt()
		);
	}
}
