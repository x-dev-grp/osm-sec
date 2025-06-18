package com.osm.securityservice.securityConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        int status;
        String errorCode;
        String message;

        if (authException instanceof BadCredentialsException) {
            status = HttpStatus.UNAUTHORIZED.value();
            errorCode = "INVALID_CREDENTIALS";
            message = "Invalid username or password.";
        } else if (authException instanceof LockedException) {
            status = HttpStatus.LOCKED.value();
            errorCode = "ACCOUNT_LOCKED";
            message = "Your account is locked.";
        } else if (authException instanceof DisabledException) {
            status = HttpStatus.FORBIDDEN.value();
            errorCode = "ACCOUNT_DISABLED";
            message = "Your account is disabled.";
        } else {
            status = HttpStatus.UNAUTHORIZED.value();
            errorCode = "AUTH_FAILED";
            message = "Authentication failed try again later.";
        }

        response.setStatus(status);

        Map<String, String> error = Map.of(
                "error", errorCode,
                "message", message
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
