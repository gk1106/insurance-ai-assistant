package com.insuranceai.backend.renewal.mapper;

import com.insuranceai.backend.renewal.dto.RenewalRequestDto;
import com.insuranceai.backend.renewal.dto.RenewalResponseDto;
import com.insuranceai.backend.renewal.entity.Renewal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RenewalMapper {

    Renewal toEntity(RenewalRequestDto dto);

    @Mapping(target = "policyId", source = "policy.id")
    @Mapping(target = "policyNumber", source = "policy.policyNumber")
    RenewalResponseDto toResponseDto(Renewal renewal);
}
