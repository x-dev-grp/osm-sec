package com.osm.securityservice.securityConfig;


import com.osm.securityservice.userManagement.models.OSMUser;
import com.osm.securityservice.userManagement.service.UserService;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
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
import java.util.stream.Collectors;

@Configuration
class CustomRefreshTokenAuthenticationProvider implements AuthenticationProvider {
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<?> tokenGenerator;
    private final UserService userService;

    public CustomRefreshTokenAuthenticationProvider(OAuth2AuthorizationService authorizationService,
                                                    OAuth2TokenGenerator<?> tokenGenerator,
                                                    UserService userService) {
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.userService = userService;
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
        OAuth2RefreshTokenAuthenticationToken refreshTokenAuth = (OAuth2RefreshTokenAuthenticationToken) authentication;

        // Find the authorization associated with the refresh token
        OAuth2Authorization authorization = authorizationService.findByToken(
                refreshTokenAuth.getRefreshToken(), OAuth2TokenType.REFRESH_TOKEN);
        OAuth2ClientAuthenticationToken clientPrincipal =
                getAuthenticatedClientElseThrowInvalidClient(refreshTokenAuth);
        RegisteredClient client = getRegisteredClient(clientPrincipal, authorization);

        assert authorization != null;
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        assert refreshToken != null;
        if (!refreshToken.isActive()) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }

        // Check if the user account is locked
        String username = authorization.getPrincipalName();
        OSMUser user = userService.getByUsername(username);

        if (user == null || user.isLocked()) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.ACCESS_DENIED);
        }

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
//        OAuth2AccessToken accessToken = (OAuth2AccessToken) tokenGenerator.generate(tokenContext);
//        if (accessToken == null) {
//            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
//        }

        // Generate access token
        OAuth2Token generatedAccessToken = tokenGenerator.generate(tokenContext);
        if (generatedAccessToken == null) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
        }
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                generatedAccessToken.getTokenValue(), generatedAccessToken.getIssuedAt(),
                generatedAccessToken.getExpiresAt(), tokenContext.getAuthorizedScopes());

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

        // Prepare additional parameters with scopes
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put(OAuth2ParameterNames.SCOPE, String.join(" ", authorization.getAuthorizedScopes()));

        // Return token response
        return new OAuth2AccessTokenAuthenticationToken(
                client,
                Objects.requireNonNull(authorization.getAttribute("principal")),
                accessToken,
                null, // No new refresh token
                additionalParameters
        );
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
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }
}