package com.osm.securityservice.securityConfig;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import java.util.*;

@Configuration
public class CustomTokenRequestConverter implements AuthenticationConverter {

    static Authentication getAuthentication(HttpServletRequest request) {
        String username = request.getParameter(OAuth2ParameterNames.USERNAME);
        String password = request.getParameter(OAuth2ParameterNames.PASSWORD);
        String scope = request.getParameter(OAuth2ParameterNames.SCOPE);

        Set<String> scopes = StringUtils.hasText(scope)
                ? new HashSet<>(Arrays.asList(scope.split(" ")))
                : Collections.emptySet();

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        return new CustomTokenGrantAuthenticationToken(
                Map.of(
                        OAuth2ParameterNames.USERNAME, username,
                        OAuth2ParameterNames.PASSWORD, password,
                        OAuth2ParameterNames.SCOPE, String.join(" ", scopes)
                ),
                clientPrincipal
        );
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!"TOKEN".equalsIgnoreCase(grantType)) {
            return null;
        }

        return getAuthentication(request);
    }
}
