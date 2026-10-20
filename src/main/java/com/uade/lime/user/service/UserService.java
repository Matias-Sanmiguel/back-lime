package com.uade.lime.user.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.lime.auth.dto.UserResponse;
import com.uade.lime.auth.model.User;
import com.uade.lime.auth.repository.UserRepository;
import com.uade.lime.common.exception.ArgumentInvalidException;
import com.uade.lime.common.exception.ProhibidoException;
import com.uade.lime.common.exception.RecursoNoEncontradoException;
import com.uade.lime.user.dto.UpdateMeRequest;

@Service
public class UserService {

    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateMe(Long userId, UpdateMeRequest request) {
        User user = findUser(userId);
        if (!hasUpdates(request)) {
            return UserResponse.from(user);
        }

        if (request.newPassword() != null) {
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                throw new ArgumentInvalidException("Ingresá tu contraseña actual para cambiarla");
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new ProhibidoException("La contraseña actual no es correcta");
            }
            if (exceedsBcryptByteLimit(request.newPassword())) {
                throw new ArgumentInvalidException("La contraseña no puede superar los 72 bytes");
            }
            user.changePassword(passwordEncoder.encode(request.newPassword()), Instant.now());
        }

        user.updateProfile(request.name(), request.agencyName(), Instant.now());
        return UserResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private boolean hasUpdates(UpdateMeRequest request) {
        return request.name() != null
                || request.agencyName() != null
                || request.newPassword() != null;
    }

    private boolean exceedsBcryptByteLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
    }
}
