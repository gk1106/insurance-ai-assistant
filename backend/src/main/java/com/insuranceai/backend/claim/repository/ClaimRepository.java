package com.insuranceai.backend.claim.repository;

import com.insuranceai.backend.claim.entity.Claim;
import com.insuranceai.backend.claim.entity.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    Optional<Claim> findByClaimNumber(String claimNumber);

    boolean existsByClaimNumber(String claimNumber);

    Page<Claim> findByPolicyId(UUID policyId, Pageable pageable);

    List<Claim> findByStatus(ClaimStatus status);

    @Query("select coalesce(sum(c.approvedAmount), 0) from Claim c " +
            "where c.policy.id = :policyId and c.status in (com.insuranceai.backend.claim.entity.ClaimStatus.APPROVED, " +
            "com.insuranceai.backend.claim.entity.ClaimStatus.PAID)")
    BigDecimal sumApprovedAmountByPolicyId(@Param("policyId") UUID policyId);
}
