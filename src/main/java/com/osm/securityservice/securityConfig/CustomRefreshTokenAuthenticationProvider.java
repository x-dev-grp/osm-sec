package com.osm.securityservice.securityConfig;


import com.osm.securityservice.userManagement.models.OSMUser;
import com.osm.securityservice.userManagement.service.UserService;
import com.xdev.xdevbase.utils.OSMLogger;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.util.*;

@Configuration
class CustomRefreshTokenAuthenticationProvider implements AuthenticationProvider {
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<?> tokenGenerator;
    private final UserService userService;

    public CustomRefreshTokenAuthenticationProvider(OAuth2AuthorizationService authorizationService,
                                                    OAuth2TokenGenerator<?> tokenGenerator,
                                                    UserService userService) {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "CustomRefreshTokenAuthenticationProvider", "Initializing refresh token authentication provider");
        
        try {
            this.authorizationService = authorizationService;
            this.tokenGenerator = tokenGenerator;
            this.userService = userService;
            
            OSMLogger.logMethodExit(this.getClass(), "CustomRefreshTokenAuthenticationProvider", "Refresh token authentication provider initialized");
            OSMLogger.logPerformance(this.getClass(), "CustomRefreshTokenAuthenticationProvider", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "REFRESH_TOKEN_PROVIDER_INITIALIZED", 
                "Custom refresh token authentication provider initialized");
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error initializing refresh token authentication provider", e);
            throw e;
        }
    }

    private static RegisteredClient getRegisteredClient(OAuth2ClientAuthenticationToken clientPrincipal, OAuth2Authorization authorization) {
        RegisteredClient client = clientPrincipal.getRegisteredClient();
        assert client != null;
        if (authorization == null) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_TOKEN);
        }

        if (!client.getId().equals(authorization.getRegisteredClientId())) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
        }

        if (!client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }
        return client;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        long startTime = System.currentTimeMillis();
        OAuth2RefreshTokenAuthenticationToken refreshTokenAuth = (OAuth2RefreshTokenAuthenticationToken) authentication;
        String refreshTokenValue = refreshTokenAuth.getRefreshToken();
        
        OSMLogger.logMethodEntry(this.getClass(), "authenticate", 
            "Refresh token authentication attempt - Token: " + (refreshTokenValue != null ? refreshTokenValue.substring(0, Math.min(10, refreshTokenValue.length())) + "..." : "null"));
        
        try {
            // Find the authorization associated with the refresh token
            OAuth2Authorization authorization = authorizationService.findByToken(
                    refreshTokenAuth.getRefreshToken(), OAuth2TokenType.REFRESH_TOKEN);
            
            if (authorization == null) {
                OSMLogger.logSecurityEvent(this.getClass(), "REFRESH_TOKEN_NOT_FOUND", 
                    "Refresh token not found in authorization service");
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_TOKEN);
            }
            
            OAuth2ClientAuthenticationToken clientPrincipal =
                    getAuthenticatedClientElseThrowInvalidClient(refreshTokenAuth);
            RegisteredClient client = getRegisteredClient(clientPrincipal, authorization);

            assert authorization != null;
            OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
            assert refreshToken != null;
            if (!refreshToken.isActive()) {
                OSMLogger.logSecurityEvent(this.getClass(), "REFRESH_TOKEN_INACTIVE", 
                    "Refresh token is not active for user: " + authorization.getPrincipalName());
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
            }

            // Check if the user account is locked
            String username = authorization.getPrincipalName();
            OSMUser user = userService.getByUsername(username);

            if (user == null) {
                OSMLogger.logSecurityEvent(this.getClass(), "REFRESH_USER_NOT_FOUND", 
                    "User not found during refresh token authentication: " + username);
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.ACCESS_DENIED);
            }
            
            if (user.isLocked()) {
                OSMLogger.logSecurityEvent(this.getClass(), "REFRESH_ACCOUNT_LOCKED", 
                    "Account locked during refresh token authentication: " + username);
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.ACCESS_DENIED);
            }

            OSMLogger.log(this.getClass(), OSMLogger.LogLevel.DEBUG, "User validation passed for refresh token: {}", username);

            // Generate new access token
            AuthorizationServerContext authorizationServerContext = AuthorizationServerContextHolder.getContext();
            Set<String> scopes = Collections.emptySet();
//        Set<String> scopes = user.getAuthorities().stream()
//                .map(GrantedAuthority::getAuthority)
//                .collect(Collectors.toSet());
            
            OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.from(authorization);
            OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                    .authorization(authorizationBuilder.build())
                    .principal(authorization.getAttribute("principal"))
                    .registeredClient(client)
                    .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                    .authorizedScopes(scopes)
                    .authorizationServerContext(authorizationServerContext)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .authorizationGrant(refreshTokenAuth)
                    .build();

            // Generate access token
            OAuth2Token generatedAccessToken = tokenGenerator.generate(tokenContext);
            if (generatedAccessToken == null) {
                OSMLogger.logSecurityEvent(this.getClass(), "REFRESH_TOKEN_GENERATION_FAILED", 
                    "Access token generation failed during refresh for user: " + username);
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
            }
            
            OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                    generatedAccessToken.getTokenValue(), generatedAccessToken.getIssuedAt(),
                    generatedAccessToken.getExpiresAt(), tokenContext.getAuthorizedScopes());

            OSMLogger.logSecurityEvent(this.getClass(), "REFRESH_ACCESS_TOKEN_GENERATED", 
                "New access token generated from refresh token for user: " + username);

            if (generatedAccessToken instanceof ClaimAccessor) {
                authorizationBuilder.token(accessToken,
                        (metadata) -> metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                                ((ClaimAccessor) generatedAccessToken).getClaims()));
            } else {
                authorizationBuilder.accessToken(accessToken);
            }

            // Save updated authorization
            OAuth2Authorization updatedAuthorization = authorizationBuilder.build();
            authorizationService.save(updatedAuthorization);
            
            OSMLogger.logSecurityEvent(this.getClass(), "REFRESH_AUTHORIZATION_UPDATED", 
                "Authorization updated with new access token for user: " + username);

            // Prepare additional parameters with scopes
            Map<String, Object> additionalParameters = new HashMap<>();
            additionalParameters.put(OAuth2ParameterNames.SCOPE, String.join(" ", authorization.getAuthorizedScopes()));

            // Return token response
            OAuth2AccessTokenAuthenticationToken result = new OAuth2AccessTokenAuthenticationToken(
                    client,
                    Objects.requireNonNull(authorization.getAttribute("principal")),
                    accessToken,
                    null, // No new refresh token
                    additionalParameters
            );
            
            OSMLogger.logMethodExit(this.getClass(), "authenticate", 
                "Refresh token authentication successful for user: " + username);
            OSMLogger.logPerformance(this.getClass(), "authenticate", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "REFRESH_TOKEN_SUCCESS", 
                "Refresh token authentication completed successfully for user: " + username);
            
            return result;
            
        } catch (OAuth2AuthenticationException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "REFRESH_TOKEN_FAILED", 
                "Refresh token authentication failed - Error: " + e.getError().getErrorCode());
            throw e;
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), 
                "Unexpected error during refresh token authentication", e);
            throw e;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2RefreshTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }


    public OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(Authentication authentication) {
        OAuth2ClientAuthenticationToken clientPrincipal = null;
        if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication.getPrincipal().getClass())) {
            clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
        }
        if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
            return clientPrincipal;
        }
        
        OSMLogger.logSecurityEvent(this.getClass(), "REFRESH_CLIENT_NOT_AUTHENTICATED", 
            "Client not authenticated during refresh token authentication");
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }
}