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
import jakarta.servlet.http.Cookie;

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
        String token = resolveToken(request);
        if (token != null) {
            try {
                Claims claims = jwtService.parse(token);
                if (!denylistedTokenRepository.existsByJti(claims.getId())) {
                    SecurityContextHolder.getContext().setAuthentication(toAuthentication(claims));
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AuthCookieService.COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
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