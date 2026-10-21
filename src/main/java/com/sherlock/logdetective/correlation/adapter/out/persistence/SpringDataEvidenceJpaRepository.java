package com.sherlock.logdetective.correlation.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataEvidenceJpaRepository extends JpaRepository<EvidenceEntity, UUID> {
}
