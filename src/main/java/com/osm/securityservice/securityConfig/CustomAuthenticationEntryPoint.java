package com.osm.securityservice.securityConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xdev.xdevbase.utils.OSMLogger;
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
        
        long startTime = System.currentTimeMillis();
        String requestURI = request.getRequestURI();
        String clientIP = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        
        OSMLogger.logMethodEntry(this.getClass(), "commence", 
            "Authentication failed for URI: " + requestURI + ", IP: " + clientIP);
        
        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            int status;
            String errorCode;
            String message;

            if (authException instanceof BadCredentialsException) {
                status = HttpStatus.UNAUTHORIZED.value();
                errorCode = "INVALID_CREDENTIALS";
                message = "Invalid username or password.";
                
                OSMLogger.logSecurityEvent(this.getClass(), "AUTH_BAD_CREDENTIALS", 
                    "Invalid credentials attempt from IP: " + clientIP + ", URI: " + requestURI);
                    
            } else if (authException instanceof LockedException) {
                status = HttpStatus.LOCKED.value();
                errorCode = "ACCOUNT_LOCKED";
                message = "Your account is locked.";
                
                OSMLogger.logSecurityEvent(this.getClass(), "AUTH_ACCOUNT_LOCKED", 
                    "Locked account access attempt from IP: " + clientIP + ", URI: " + requestURI);
                    
            } else if (authException instanceof DisabledException) {
                status = HttpStatus.FORBIDDEN.value();
                errorCode = "ACCOUNT_DISABLED";
                message = "Your account is disabled.";
                
                OSMLogger.logSecurityEvent(this.getClass(), "AUTH_ACCOUNT_DISABLED", 
                    "Disabled account access attempt from IP: " + clientIP + ", URI: " + requestURI);
                    
            } else {
                status = HttpStatus.UNAUTHORIZED.value();
                errorCode = "AUTH_FAILED";
                message = "Authentication failed try again later.";
                
                OSMLogger.logSecurityEvent(this.getClass(), "AUTH_GENERAL_FAILURE", 
                    "General authentication failure from IP: " + clientIP + ", URI: " + requestURI + 
                    ", Exception: " + authException.getClass().getSimpleName());
            }

            response.setStatus(status);

            Map<String, String> error = Map.of(
                    "error", errorCode,
                    "message", message
            );

            String errorResponse = objectMapper.writeValueAsString(error);
            response.getWriter().write(errorResponse);
            
            OSMLogger.logMethodExit(this.getClass(), "commence", 
                "Authentication failure handled - Status: " + status + ", Error: " + errorCode);
            OSMLogger.logPerformance(this.getClass(), "commence", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "AUTH_RESPONSE_SENT", 
                "Authentication failure response sent - Status: " + status + ", IP: " + clientIP);
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), 
                "Error handling authentication failure for URI: " + requestURI + ", IP: " + clientIP, e);
            throw e;
        }
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
}
