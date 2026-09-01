package com.insuranceai.backend.auth.service;

import com.insuranceai.backend.auth.dto.AuthResponseDto;
import com.insuranceai.backend.auth.dto.LoginRequestDto;
import com.insuranceai.backend.auth.dto.RegisterRequestDto;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto request);

    AuthResponseDto login(LoginRequestDto request);
}
