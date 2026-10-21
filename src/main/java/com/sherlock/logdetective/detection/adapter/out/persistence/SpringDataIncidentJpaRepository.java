package com.sherlock.logdetective.detection.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataIncidentJpaRepository extends JpaRepository<IncidentEntity, UUID> {

	Optional<IncidentEntity> findByFingerprint_Fingerprint(String fingerprint);

	Page<IncidentEntity> findByStatus(String status, Pageable pageable);
}
