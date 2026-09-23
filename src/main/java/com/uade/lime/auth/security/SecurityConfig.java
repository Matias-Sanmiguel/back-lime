package com.uade.lime.auth.security;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.uade.lime.auth.repository.DenylistedTokenRepository;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            DenylistedTokenRepository denylistedTokenRepository) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtService, denylistedTokenRepository);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/properties", "/api/v1/properties/{id}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/properties/{id}/inquiries").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/properties").hasAnyRole("AGENCY", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/properties/{id}").hasAnyRole("AGENCY", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/properties/{id}").hasAnyRole("AGENCY", "ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/properties/{id}/publish",
                                "/api/v1/properties/{id}/pause",
                                "/api/v1/properties/{id}/images")
                        .hasAnyRole("AGENCY", "ADMIN")
                        .requestMatchers(
                                HttpMethod.PATCH, "/api/v1/properties/{id}/images/{imageId}")
                        .hasAnyRole("AGENCY", "ADMIN")
                        .requestMatchers(
                                HttpMethod.DELETE, "/api/v1/properties/{id}/images/{imageId}")
                        .hasAnyRole("AGENCY", "ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> writeProblemDetail(
                                response, HttpStatus.UNAUTHORIZED,
                                "Authentication is required to access this resource"))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeProblemDetail(
                                response, HttpStatus.FORBIDDEN,
                                "You do not have permission to access this resource")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Los rechazos de la capa de seguridad ocurren antes del DispatcherServlet, así que
    // ApiExceptionHandler (@RestControllerAdvice) nunca los ve. Se arma el JSON a mano
    // (en vez de reusar el ObjectMapper de Spring) para no atarse a qué major de Jackson
    // esté resolviendo el proyecto en cada momento.
    private void writeProblemDetail(HttpServletResponse response, HttpStatus status, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String body = "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\"}"
                .formatted(status.getReasonPhrase(), status.value(), escapeJson(detail));
        response.getWriter().write(body);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
