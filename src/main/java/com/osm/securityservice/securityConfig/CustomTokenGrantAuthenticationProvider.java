package com.osm.securityservice.securityConfig;

import com.osm.securityservice.userManagement.models.OSMUser;
import com.osm.securityservice.userManagement.service.UserService;
import com.xdev.xdevbase.utils.OSMLogger;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
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
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "CustomTokenGrantAuthenticationProvider", "Initializing token grant authentication provider");
        
        try {
            this.authenticationManager = authenticationManager;
            this.authorizationService = authorizationService;
            this.tokenGenerator = tokenGenerator;
            this.registeredClientRepository = registeredClientRepository;
            this.userService = userService;
            
            OSMLogger.logMethodExit(this.getClass(), "CustomTokenGrantAuthenticationProvider", "Token grant authentication provider initialized");
            OSMLogger.logPerformance(this.getClass(), "CustomTokenGrantAuthenticationProvider", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "TOKEN_GRANT_PROVIDER_INITIALIZED", 
                "Custom token grant authentication provider initialized");
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error initializing token grant authentication provider", e);
            throw e;
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        long startTime = System.currentTimeMillis();
        CustomTokenGrantAuthenticationToken tokenAuth = (CustomTokenGrantAuthenticationToken) authentication;
        String username = tokenAuth.getUsername();
        String clientId = ((Authentication) tokenAuth.getPrincipal()).getName();
        
        OSMLogger.logMethodEntry(this.getClass(), "authenticate", 
            "Token grant authentication attempt - Username: " + username + ", Client: " + clientId);
        
        try {
            OSMUser user = userService.getByUsername(tokenAuth.getUsername());
            
            if (user == null) {
                OSMLogger.logSecurityEvent(this.getClass(), "AUTH_USER_NOT_FOUND", 
                    "Authentication failed - User not found: " + username);
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.ACCESS_DENIED);
            }
            
            if (user.isLocked()) {
                OSMLogger.logSecurityEvent(this.getClass(), "AUTH_ACCOUNT_LOCKED", 
                    "Authentication failed - Account locked: " + username);
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.ACCESS_DENIED);
            }
            
            if (user.isNewUser()) {
                OSMLogger.logSecurityEvent(this.getClass(), "AUTH_NEW_USER_ACCESS_DENIED", 
                    "Authentication failed - New user access denied: " + username);
                throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED, user.getUsername(), user.getId().toString()));
            }
            
            OSMLogger.log(this.getClass(), OSMLogger.LogLevel.DEBUG, "User validation passed for: {}", username);
            
            // Authenticate user with username and password
            Authentication userAuth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(tokenAuth.getUsername(), tokenAuth.getPassword())
            );
            
            OSMLogger.logSecurityEvent(this.getClass(), "AUTH_USER_AUTHENTICATED", 
                "User authenticated successfully: " + username);

            // Validate client
            RegisteredClient client = registeredClientRepository.findByClientId(clientId);
            if (client == null) {
                OSMLogger.logSecurityEvent(this.getClass(), "AUTH_CLIENT_NOT_FOUND", 
                    "Authentication failed - Client not found: " + clientId);
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
            }
            
            OSMLogger.log(this.getClass(), OSMLogger.LogLevel.DEBUG, "Client validation passed: {}", clientId);
            
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
                OSMLogger.logSecurityEvent(this.getClass(), "AUTH_CONTEXT_MISSING", 
                    "Authentication failed - AuthorizationServerContext not available for user: " + username);
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
                OSMLogger.logSecurityEvent(this.getClass(), "AUTH_TOKEN_GENERATION_FAILED", 
                    "Access token generation failed for user: " + username);
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
            }
            
            OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                    generatedAccessToken.getTokenValue(), generatedAccessToken.getIssuedAt(),
                    generatedAccessToken.getExpiresAt(), tokenContext.getAuthorizedScopes());
            
            OSMLogger.logSecurityEvent(this.getClass(), "AUTH_ACCESS_TOKEN_GENERATED", 
                "Access token generated successfully for user: " + username);
            
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
                OSMLogger.logSecurityEvent(this.getClass(), "AUTH_REFRESH_TOKEN_GENERATION_FAILED", 
                    "Refresh token generation failed for user: " + username);
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
            }
            
            authorizationBuilder.refreshToken(refreshToken);
            OSMLogger.logSecurityEvent(this.getClass(), "AUTH_REFRESH_TOKEN_GENERATED", 
                "Refresh token generated successfully for user: " + username);

            // Save authorization
            OAuth2Authorization authorization = authorizationBuilder.build();
            authorizationService.save(authorization);
            
            OSMLogger.logSecurityEvent(this.getClass(), "AUTH_AUTHORIZATION_SAVED", 
                "Authorization saved successfully for user: " + username);

            // Prepare additional parameters with scopes
            Map<String, Object> additionalParameters = new HashMap<>();
            additionalParameters.put(OAuth2ParameterNames.SCOPE, String.join(" ", tokenAuth.getScopes()));

            // Return token response
            OAuth2AccessTokenAuthenticationToken result = new OAuth2AccessTokenAuthenticationToken(
                    client,
                    userAuth,
                    accessToken,
                    refreshToken,
                    additionalParameters
            );
            
            OSMLogger.logMethodExit(this.getClass(), "authenticate", 
                "Token grant authentication successful for user: " + username);
            OSMLogger.logPerformance(this.getClass(), "authenticate", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "AUTH_TOKEN_GRANT_SUCCESS", 
                "Token grant authentication completed successfully - User: " + username + ", Client: " + clientId);
            
            return result;
            
        } catch (OAuth2AuthenticationException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "AUTH_TOKEN_GRANT_FAILED", 
                "Token grant authentication failed - User: " + username + ", Client: " + clientId + 
                ", Error: " + e.getError().getErrorCode());
            throw e;
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), 
                "Unexpected error during token grant authentication for user: " + username + ", client: " + clientId, e);
            throw e;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CustomTokenGrantAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

