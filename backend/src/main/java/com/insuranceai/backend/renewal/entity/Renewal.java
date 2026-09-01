package com.insuranceai.backend.renewal.entity;

import com.insuranceai.backend.common.entity.BaseEntity;
import com.insuranceai.backend.policy.entity.Policy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "renewals")
public class Renewal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "previous_end_date", nullable = false)
    private LocalDate previousEndDate;

    @Column(name = "new_end_date", nullable = false)
    private LocalDate newEndDate;

    @Column(name = "revised_premium_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal revisedPremiumAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RenewalStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public LocalDate getPreviousEndDate() {
        return previousEndDate;
    }

    public void setPreviousEndDate(LocalDate previousEndDate) {
        this.previousEndDate = previousEndDate;
    }

    public LocalDate getNewEndDate() {
        return newEndDate;
    }

    public void setNewEndDate(LocalDate newEndDate) {
        this.newEndDate = newEndDate;
    }

    public BigDecimal getRevisedPremiumAmount() {
        return revisedPremiumAmount;
    }

    public void setRevisedPremiumAmount(BigDecimal revisedPremiumAmount) {
        this.revisedPremiumAmount = revisedPremiumAmount;
    }

    public RenewalStatus getStatus() {
        return status;
    }

    public void setStatus(RenewalStatus status) {
        this.status = status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }
}
