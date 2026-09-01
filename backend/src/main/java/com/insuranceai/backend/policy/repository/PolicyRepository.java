package com.insuranceai.backend.policy.repository;

import com.insuranceai.backend.policy.entity.Policy;
import com.insuranceai.backend.policy.entity.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    Optional<Policy> findByPolicyNumber(String policyNumber);

    boolean existsByPolicyNumber(String policyNumber);

    Page<Policy> findByCustomerId(UUID customerId, Pageable pageable);

    List<Policy> findByStatus(PolicyStatus status);

    List<Policy> findByEndDateBetween(LocalDate start, LocalDate end);
}
