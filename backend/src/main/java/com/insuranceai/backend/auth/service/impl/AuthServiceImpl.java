package com.insuranceai.backend.auth.service.impl;

import com.insuranceai.backend.auth.dto.AuthResponseDto;
import com.insuranceai.backend.auth.dto.LoginRequestDto;
import com.insuranceai.backend.auth.dto.RegisterRequestDto;
import com.insuranceai.backend.auth.entity.Role;
import com.insuranceai.backend.auth.entity.User;
import com.insuranceai.backend.auth.repository.UserRepository;
import com.insuranceai.backend.auth.service.AuthService;
import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import com.insuranceai.backend.common.exception.ResourceNotFoundException;
import com.insuranceai.backend.customer.entity.Customer;
import com.insuranceai.backend.customer.repository.CustomerRepository;
import com.insuranceai.backend.security.JwtService;
import com.insuranceai.backend.security.UserPrincipal;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                            CustomerRepository customerRepository,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessRuleViolationException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleViolationException("Email is already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);

        if (request.customerId() != null) {
            Customer customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));
            user.setCustomer(customer);
        }

        user = userRepository.save(user);
        return issueToken(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }

        return issueToken(user);
    }

    private AuthResponseDto issueToken(User user) {
        UserPrincipal principal = UserPrincipal.fromUser(user);
        String token = jwtService.generateToken(principal);
        return new AuthResponseDto(token, user.getUsername(), user.getRole().name(), jwtService.extractExpiration(token));
    }
}
