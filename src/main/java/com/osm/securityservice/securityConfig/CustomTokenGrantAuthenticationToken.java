package com.osm.securityservice.securityConfig;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class CustomTokenGrantAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {
    private final String username;
    private final String password;
    private final Set<String> scopes;

    public CustomTokenGrantAuthenticationToken(@Nullable Map<String, Object> additionalParameters, Authentication authentication) {
        super(new AuthorizationGrantType("TOKEN"), authentication, additionalParameters
        );
        assert additionalParameters != null;
        this.username = (String) additionalParameters.get("username");
        this.password = (String) additionalParameters.get("password");
        String scope = (String) additionalParameters.get(OAuth2ParameterNames.SCOPE);
        this.scopes = StringUtils.hasText(scope) ?
                new HashSet<>(Arrays.asList(scope.split(" "))) : Set.of("read", "write");
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Set<String> getScopes() {
        return scopes;
    }
}