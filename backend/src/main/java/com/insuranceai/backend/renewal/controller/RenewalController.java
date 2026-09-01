package com.insuranceai.backend.renewal.controller;

import com.insuranceai.backend.renewal.dto.RenewalRequestDto;
import com.insuranceai.backend.renewal.dto.RenewalResponseDto;
import com.insuranceai.backend.renewal.service.RenewalService;
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
@RequestMapping("/api/renewals")
public class RenewalController {

    private final RenewalService renewalService;

    public RenewalController(RenewalService renewalService) {
        this.renewalService = renewalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RenewalResponseDto create(@Valid @RequestBody RenewalRequestDto request) {
        return renewalService.create(request);
    }

    @GetMapping("/{id}")
    public RenewalResponseDto getById(@PathVariable UUID id) {
        return renewalService.getById(id);
    }

    @GetMapping
    public Page<RenewalResponseDto> listByPolicy(@RequestParam UUID policyId, Pageable pageable) {
        return renewalService.getByPolicy(policyId, pageable);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public RenewalResponseDto confirm(@PathVariable UUID id) {
        return renewalService.confirm(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public RenewalResponseDto reject(@PathVariable UUID id) {
        return renewalService.reject(id);
    }
}
