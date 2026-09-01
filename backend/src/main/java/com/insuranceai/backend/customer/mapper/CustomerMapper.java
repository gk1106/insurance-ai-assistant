package com.insuranceai.backend.customer.mapper;

import com.insuranceai.backend.customer.dto.CustomerRequestDto;
import com.insuranceai.backend.customer.dto.CustomerResponseDto;
import com.insuranceai.backend.customer.entity.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    Customer toEntity(CustomerRequestDto dto);

    CustomerResponseDto toResponseDto(Customer customer);
}
