package com.insuranceai.backend.customer.service;

import com.insuranceai.backend.customer.dto.CustomerRequestDto;
import com.insuranceai.backend.customer.dto.CustomerResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    CustomerResponseDto create(CustomerRequestDto request);

    CustomerResponseDto getById(UUID id);

    Page<CustomerResponseDto> getAll(Pageable pageable);

    CustomerResponseDto update(UUID id, CustomerRequestDto request);

    void delete(UUID id);
}
