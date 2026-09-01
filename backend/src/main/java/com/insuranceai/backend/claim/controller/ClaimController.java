package com.insuranceai.backend.claim.controller;

import com.insuranceai.backend.claim.dto.ClaimApproveRequestDto;
import com.insuranceai.backend.claim.dto.ClaimRejectRequestDto;
import com.insuranceai.backend.claim.dto.ClaimRequestDto;
import com.insuranceai.backend.claim.dto.ClaimResponseDto;
import com.insuranceai.backend.claim.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimResponseDto create(@Valid @RequestBody ClaimRequestDto request) {
        return claimService.create(request);
    }

    @GetMapping("/{id}")
    public ClaimResponseDto getById(@PathVariable UUID id) {
        return claimService.getById(id);
    }

    @GetMapping(params = "!policyId")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public Page<ClaimResponseDto> listAll(Pageable pageable) {
        return claimService.getAll(pageable);
    }

    @GetMapping(params = "policyId")
    public Page<ClaimResponseDto> listByPolicy(@RequestParam UUID policyId, Pageable pageable) {
        return claimService.getByPolicy(policyId, pageable);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ClaimResponseDto review(@PathVariable UUID id) {
        return claimService.review(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ClaimResponseDto approve(@PathVariable UUID id, @Valid @RequestBody ClaimApproveRequestDto request) {
        return claimService.approve(id, request);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ClaimResponseDto reject(@PathVariable UUID id, @Valid @RequestBody ClaimRejectRequestDto request) {
        return claimService.reject(id, request);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ClaimResponseDto pay(@PathVariable UUID id) {
        return claimService.markPaid(id);
    }
}
