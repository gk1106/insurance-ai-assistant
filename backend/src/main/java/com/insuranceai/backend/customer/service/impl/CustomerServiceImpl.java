package com.insuranceai.backend.customer.service.impl;

import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import com.insuranceai.backend.common.exception.ResourceNotFoundException;
import com.insuranceai.backend.customer.dto.CustomerRequestDto;
import com.insuranceai.backend.customer.dto.CustomerResponseDto;
import com.insuranceai.backend.customer.entity.Customer;
import com.insuranceai.backend.customer.mapper.CustomerMapper;
import com.insuranceai.backend.customer.repository.CustomerRepository;
import com.insuranceai.backend.customer.service.CustomerService;
import com.insuranceai.backend.policy.repository.PolicyRepository;
import com.insuranceai.backend.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final PolicyRepository policyRepository;
    private final CustomerMapper customerMapper;
    private final CurrentUserProvider currentUserProvider;

    public CustomerServiceImpl(CustomerRepository customerRepository,
                                PolicyRepository policyRepository,
                                CustomerMapper customerMapper,
                                CurrentUserProvider currentUserProvider) {
        this.customerRepository = customerRepository;
        this.policyRepository = policyRepository;
        this.customerMapper = customerMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public CustomerResponseDto create(CustomerRequestDto request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new BusinessRuleViolationException("A customer with this email already exists");
        }
        Customer customer = customerMapper.toEntity(request);
        customer = customerRepository.save(customer);
        return customerMapper.toResponseDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getById(UUID id) {
        Customer customer = findCustomerOrThrow(id);
        currentUserProvider.assertOwnerOrElevated(customer.getId());
        return customerMapper.toResponseDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> getAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(customerMapper::toResponseDto);
    }

    @Override
    @Transactional
    public CustomerResponseDto update(UUID id, CustomerRequestDto request) {
        Customer customer = findCustomerOrThrow(id);
        if (!customer.getEmail().equalsIgnoreCase(request.email()) && customerRepository.existsByEmail(request.email())) {
            throw new BusinessRuleViolationException("A customer with this email already exists");
        }

        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        customer.setPhoneNumber(request.phoneNumber());
        customer.setDateOfBirth(request.dateOfBirth());
        customer.setAddressLine(request.addressLine());
        customer.setCity(request.city());
        customer.setPostalCode(request.postalCode());
        customer.setCountry(request.country());

        customer = customerRepository.save(customer);
        return customerMapper.toResponseDto(customer);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Customer customer = findCustomerOrThrow(id);
        boolean hasPolicies = policyRepository.findByCustomerId(id, Pageable.unpaged()).hasContent();
        if (hasPolicies) {
            throw new BusinessRuleViolationException("Cannot delete a customer with existing policies");
        }
        customerRepository.delete(customer);
    }

    private Customer findCustomerOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }
}
