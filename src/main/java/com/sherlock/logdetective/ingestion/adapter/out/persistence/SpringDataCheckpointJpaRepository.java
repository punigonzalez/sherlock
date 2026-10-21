package com.sherlock.logdetective.ingestion.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCheckpointJpaRepository extends JpaRepository<SourceCheckpointEntity, String> {
}
