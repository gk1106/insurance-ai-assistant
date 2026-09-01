package com.insuranceai.backend.claim.mapper;

import com.insuranceai.backend.claim.dto.ClaimRequestDto;
import com.insuranceai.backend.claim.dto.ClaimResponseDto;
import com.insuranceai.backend.claim.entity.Claim;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClaimMapper {

    Claim toEntity(ClaimRequestDto dto);

    @Mapping(target = "policyId", source = "policy.id")
    @Mapping(target = "policyNumber", source = "policy.policyNumber")
    ClaimResponseDto toResponseDto(Claim claim);
}
