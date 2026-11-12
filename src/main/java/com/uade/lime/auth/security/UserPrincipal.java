package com.uade.lime.auth.security;

import com.uade.lime.auth.model.User;
import com.uade.lime.auth.model.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public record UserPrincipal(
        Long id,
        String email,
        String name,
        UserRole role,
        String agencyName,
        String jti,
        Instant expiresAt
) implements UserDetails {

    // Constructor secundario para soportar que AuthService pase milisegundos en Long
    public UserPrincipal(Long id, String email, String name, UserRole role, String agencyName, String jti, Long expiresAt) {
        this(id, email, name, role, agencyName, jti, expiresAt != null ? Instant.ofEpochMilli(expiresAt) : null);
    }

    // Método de conveniencia para crear el principal desde la entidad User
    public static UserPrincipal from(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getAgencyName(),
                null,
                (Instant) null
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return ""; // La contraseña no viaja en el JWT
    }

    @Override
    public String getUsername() {
        return email; // Mapeado al email según el enunciado
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}