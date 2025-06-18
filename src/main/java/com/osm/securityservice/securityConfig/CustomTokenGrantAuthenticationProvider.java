package com.osm.securityservice.securityConfig;

import com.osm.securityservice.userManagement.models.OSMUser;
import com.osm.securityservice.userManagement.service.UserService;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
class CustomTokenGrantAuthenticationProvider implements AuthenticationProvider {
    private final AuthenticationManager authenticationManager;
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<?> tokenGenerator;
    private final RegisteredClientRepository registeredClientRepository;
    private final UserService userService;

    public CustomTokenGrantAuthenticationProvider(AuthenticationManager authenticationManager,
                                                  OAuth2AuthorizationService authorizationService,
                                                  OAuth2TokenGenerator<?> tokenGenerator, RegisteredClientRepository registeredClientRepository, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.registeredClientRepository = registeredClientRepository;
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        CustomTokenGrantAuthenticationToken tokenAuth = (CustomTokenGrantAuthenticationToken) authentication;
        OSMUser user = userService.getByUsername(tokenAuth.getUsername());

        if (user == null || user.isLocked()) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.ACCESS_DENIED);
        }
        if (user.isNewUser()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED, user.getUsername(), user.getId().toString()));
        }
        // Authenticate user with username and password
        Authentication userAuth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(tokenAuth.getUsername(), tokenAuth.getPassword())
        );


        // Validate client
        Authentication clientPrincipal = (Authentication) tokenAuth.getPrincipal();
        String clientId = clientPrincipal.getName();

        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        Set<String> scopes = Collections.emptySet();
//        Set<String> scopes = user.getAuthorities().stream()
//                .map(GrantedAuthority::getAuthority)
//                .collect(Collectors.toSet());
        // Create OAuth2 authorization
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(client)
                .principalName(userAuth.getName())
                .authorizationGrantType(new AuthorizationGrantType("TOKEN"))
                .authorizedScopes(scopes)
                .attribute("principal", userAuth);
        // Get the AuthorizationServerContext
        AuthorizationServerContext authorizationServerContext = AuthorizationServerContextHolder.getContext();
        if (authorizationServerContext == null) {
            throw new IllegalStateException("AuthorizationServerContext is not available");
        }
        // Create OAuth2TokenContext for token generation
        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .authorization(authorizationBuilder.build())
                .principal(userAuth)
                .registeredClient(client)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizedScopes(scopes)
                .authorizationServerContext(authorizationServerContext)
                .authorizationGrantType(new AuthorizationGrantType("TOKEN"))// Set the AuthorizationServerContext
                .authorizationGrant(tokenAuth)
                .build();

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


        // Generate refresh token
        OAuth2TokenContext refreshTokenContext = DefaultOAuth2TokenContext.builder()
                .authorization(authorizationBuilder.build())
                .principal(userAuth)
                .registeredClient(client)
                .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                .authorizedScopes(scopes)
                .authorizationServerContext(authorizationServerContext)
                .authorizationGrantType(new AuthorizationGrantType("TOKEN"))
                .authorizationGrant(tokenAuth)
                .build();
        OAuth2RefreshToken refreshToken = (OAuth2RefreshToken) tokenGenerator.generate(refreshTokenContext);
        if (refreshToken == null) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
        }
        authorizationBuilder.refreshToken(refreshToken);

        // Save authorization
        OAuth2Authorization authorization = authorizationBuilder.build();
        authorizationService.save(authorization);

        // Prepare additional parameters with scopes
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put(OAuth2ParameterNames.SCOPE, String.join(" ", tokenAuth.getScopes()));

        // Return token response
        return new OAuth2AccessTokenAuthenticationToken(
                client,
                userAuth,
                accessToken,
                refreshToken,
                additionalParameters
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CustomTokenGrantAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

