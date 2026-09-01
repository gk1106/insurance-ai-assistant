package com.insuranceai.backend.policy.controller;

import com.insuranceai.backend.policy.dto.PolicyRenewRequestDto;
import com.insuranceai.backend.policy.dto.PolicyRequestDto;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;
import com.insuranceai.backend.policy.service.PolicyService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;
    private final RenewalService renewalService;

    public PolicyController(PolicyService policyService, RenewalService renewalService) {
        this.policyService = policyService;
        this.renewalService = renewalService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyResponseDto create(@Valid @RequestBody PolicyRequestDto request) {
        return policyService.create(request);
    }

    @GetMapping("/{id}")
    public PolicyResponseDto getById(@PathVariable UUID id) {
        return policyService.getById(id);
    }

    @GetMapping("/number/{policyNumber}")
    public PolicyResponseDto getByPolicyNumber(@PathVariable String policyNumber) {
        return policyService.getByPolicyNumber(policyNumber);
    }

    @GetMapping(params = "!customerId")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public Page<PolicyResponseDto> listAll(Pageable pageable) {
        return policyService.getAll(pageable);
    }

    @GetMapping(params = "customerId")
    public Page<PolicyResponseDto> listByCustomer(@RequestParam UUID customerId, Pageable pageable) {
        return policyService.getByCustomer(customerId, pageable);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public PolicyResponseDto update(@PathVariable UUID id, @Valid @RequestBody PolicyRequestDto request) {
        return policyService.update(id, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public PolicyResponseDto cancel(@PathVariable UUID id) {
        return policyService.cancel(id);
    }

    @PostMapping("/{id}/renew")
    public RenewalResponseDto renew(@PathVariable UUID id, @Valid @RequestBody PolicyRenewRequestDto request) {
        RenewalRequestDto renewalRequest = new RenewalRequestDto(id, request.newEndDate(), request.revisedPremiumAmount());
        return renewalService.create(renewalRequest);
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public List<PolicyResponseDto> expiring(@RequestParam(defaultValue = "30") int days) {
        return policyService.getExpiringWithin(days);
    }
}
