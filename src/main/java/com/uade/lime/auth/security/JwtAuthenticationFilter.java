package com.uade.lime.auth.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.uade.lime.auth.model.UserRole;
import com.uade.lime.auth.repository.DenylistedTokenRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final DenylistedTokenRepository denylistedTokenRepository;

    public JwtAuthenticationFilter(JwtService jwtService, DenylistedTokenRepository denylistedTokenRepository) {
        this.jwtService = jwtService;
        this.denylistedTokenRepository = denylistedTokenRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(header.substring(7));
                if (!denylistedTokenRepository.existsByJti(claims.getId())) {
                    SecurityContextHolder.getContext().setAuthentication(toAuthentication(claims));
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken toAuthentication(Claims claims) {
        UserPrincipal principal = new UserPrincipal(
                Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("name", String.class),
                UserRole.valueOf(claims.get("role", String.class)),
                claims.get("agencyName", String.class),
                claims.getId(),
                claims.getExpiration().toInstant());

        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }
}