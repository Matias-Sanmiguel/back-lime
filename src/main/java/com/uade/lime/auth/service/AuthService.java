package com.uade.lime.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.uade.lime.common.ArgumentInvalidException;

import com.uade.lime.auth.dto.AuthResponse;
import com.uade.lime.auth.dto.LoginRequest;
import com.uade.lime.auth.dto.RegisterRequest;
import com.uade.lime.auth.dto.UserResponse;
import com.uade.lime.auth.model.DenylistedToken;
import com.uade.lime.auth.model.User;
import com.uade.lime.auth.model.UserRole;
import com.uade.lime.auth.repository.DenylistedTokenRepository;
import com.uade.lime.auth.repository.UserRepository;
import com.uade.lime.auth.security.JwtService;
import com.uade.lime.auth.security.UserPrincipal;

@Service
public class AuthService {

    private static final String BAD_CREDENTIALS_MESSAGE = "Invalid email or password";
    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final DenylistedTokenRepository denylistedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            DenylistedTokenRepository denylistedTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.denylistedTokenRepository = denylistedTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UserRole role = request.role() != null ? request.role() : UserRole.USER;
        if (role == UserRole.ADMIN) {
            throw new ArgumentInvalidException("Cannot register a user with role ADMIN");
        }
        if (exceedsBcryptByteLimit(request.password())) {
            throw new ArgumentInvalidException("password must be at most 72 bytes when UTF-8 encoded");
        }

        String agencyName = null;
        if (role == UserRole.AGENCY) {
            if (request.agencyName() == null || request.agencyName().isBlank()) {
                throw new ArgumentInvalidException("agencyName is required for role AGENCY");
            }
            agencyName = request.agencyName();
        }

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = User.register(
                email,
                passwordEncoder.encode(request.password()),
                request.name(),
                role,
                agencyName,
                request.birthDate(),
                request.sex(),
                Instant.now());
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        return issueAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (exceedsBcryptByteLimit(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS_MESSAGE);
        }
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS_MESSAGE));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS_MESSAGE);
        }
        return issueAuthResponse(user);
    }

    @Transactional
    public void logout(UserPrincipal principal) {
        denylistedTokenRepository.save(
                DenylistedToken.of(principal.jti(), principal.expiresAt(), Instant.now()));
    }

    private AuthResponse issueAuthResponse(User user) {
        String token = jwtService.issue(user, UUID.randomUUID().toString());
        return AuthResponse.of(token, JwtService.EXPIRATION_SECONDS, UserResponse.from(user));
    }

    private boolean exceedsBcryptByteLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
