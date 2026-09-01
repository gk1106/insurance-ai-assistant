package com.insuranceai.backend.renewal.repository;

import com.insuranceai.backend.renewal.entity.Renewal;
import com.insuranceai.backend.renewal.entity.RenewalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RenewalRepository extends JpaRepository<Renewal, UUID> {

    Page<Renewal> findByPolicyId(UUID policyId, Pageable pageable);

    List<Renewal> findByStatus(RenewalStatus status);

    Optional<Renewal> findTopByPolicyIdOrderByRequestedAtDesc(UUID policyId);
}
