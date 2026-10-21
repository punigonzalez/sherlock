package com.sherlock.logdetective.detection.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataFingerprintJpaRepository extends JpaRepository<ErrorFingerprintEntity, String> {
}
