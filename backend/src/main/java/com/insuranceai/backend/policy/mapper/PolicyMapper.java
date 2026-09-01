package com.insuranceai.backend.policy.mapper;

import com.insuranceai.backend.policy.dto.PolicyRequestDto;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;
import com.insuranceai.backend.policy.entity.Policy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PolicyMapper {

    Policy toEntity(PolicyRequestDto dto);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", expression = "java(policy.getCustomer().getFirstName() + \" \" + policy.getCustomer().getLastName())")
    PolicyResponseDto toResponseDto(Policy policy);
}
