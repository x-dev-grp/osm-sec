package com.osm.securityservice.securityConfig;

import com.osm.securityservice.userManagement.service.UserService;
import com.xdev.xdevbase.utils.OSMLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AuthServerConfig {

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<?> tokenGenerator;
    private final AuthenticationManager authenticationManager;
    private final CustomTokenRequestConverter customTokenRequestConverter;
    private final UserService userService;

    public AuthServerConfig(AuthenticationEntryPoint authenticationEntryPoint,
                            RegisteredClientRepository registeredClientRepository,
                            OAuth2AuthorizationService authorizationService,
                            OAuth2TokenGenerator<?> tokenGenerator,
                            AuthenticationManager authenticationManager,
                            CustomTokenRequestConverter customTokenRequestConverter,
                            UserService userService) {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "AuthServerConfig", "Initializing AuthServerConfig");
        
        try {
            this.authenticationEntryPoint = authenticationEntryPoint;
            this.registeredClientRepository = registeredClientRepository;
            this.authorizationService = authorizationService;
            this.tokenGenerator = tokenGenerator;
            this.authenticationManager = authenticationManager;
            this.customTokenRequestConverter = customTokenRequestConverter;
            this.userService = userService;
            
            OSMLogger.logMethodExit(this.getClass(), "AuthServerConfig", "AuthServerConfig initialized successfully");
            OSMLogger.logPerformance(this.getClass(), "AuthServerConfig", startTime, System.currentTimeMillis());
            OSMLogger.logBusinessEvent(this.getClass(), "AUTH_SERVER_CONFIG_INITIALIZED", 
                "OAuth2 Authorization Server configuration initialized");
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error initializing AuthServerConfig", e);
            throw e;
        }
    }

    // Public endpoints that require no authentication
    @Bean
    @Order(1)
    public SecurityFilterChain publicEndpointsFilterChain(HttpSecurity http) throws Exception {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "publicEndpointsFilterChain", "Configuring public endpoints");
        
        try {
            http
                    .securityMatcher("/api/security/user/auth/**")
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(csrf -> csrf.disable())
                    .cors(cors -> cors.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

            SecurityFilterChain filterChain = http.build();
            
            OSMLogger.logMethodExit(this.getClass(), "publicEndpointsFilterChain", "Public endpoints filter chain configured");
            OSMLogger.logPerformance(this.getClass(), "publicEndpointsFilterChain", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "PUBLIC_ENDPOINTS_CONFIGURED", 
                "Public endpoints filter chain configured for /api/security/user/auth/**");
            
            return filterChain;
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error configuring public endpoints filter chain", e);
            throw e;
        }
    }

    // Main security filter chain for OAuth2 server and secured APIs
    @Bean
    @Order(2)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "authorizationServerSecurityFilterChain", "Configuring OAuth2 authorization server");
        
        try {
            OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = getOAuth2AuthorizationServerConfigurer();

            http
                    .securityMatcher("/oauth2/**", "/jwks", "/.well-known/**", "/api/security/**")
                    .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(
                                    "/oauth2/**",
                                    "/jwks",
                                    "/.well-known/**",
                                    "/v3/api-docs/**",
                                    "/swagger-ui/**",
                                    "/swagger-resources/**"
                            ).permitAll()
                            .anyRequest().authenticated()
                    )
                    .csrf(csrf -> csrf.disable())
                    .cors(cors -> cors.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                    .with(authorizationServerConfigurer, configurer -> {
                    })
                    .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));

            SecurityFilterChain filterChain = http.build();
            
            OSMLogger.logMethodExit(this.getClass(), "authorizationServerSecurityFilterChain", "OAuth2 authorization server configured");
            OSMLogger.logPerformance(this.getClass(), "authorizationServerSecurityFilterChain", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "OAUTH2_SERVER_CONFIGURED", 
                "OAuth2 Authorization Server security filter chain configured");
            
            return filterChain;
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error configuring OAuth2 authorization server filter chain", e);
            throw e;
        }
    }

    private OAuth2AuthorizationServerConfigurer getOAuth2AuthorizationServerConfigurer() {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "getOAuth2AuthorizationServerConfigurer", "Creating OAuth2 authorization server configurer");
        
        try {
            OAuth2AuthorizationServerConfigurer configurer = new OAuth2AuthorizationServerConfigurer();
            configurer
                    .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                            .accessTokenRequestConverter(customTokenRequestConverter)
                            .authenticationProvider(new CustomTokenGrantAuthenticationProvider(
                                    authenticationManager, authorizationService, tokenGenerator,
                                    registeredClientRepository, userService))
                            .authenticationProvider(new CustomRefreshTokenAuthenticationProvider(
                                    authorizationService, tokenGenerator, userService))
                    );
            
            OSMLogger.logMethodExit(this.getClass(), "getOAuth2AuthorizationServerConfigurer", "OAuth2 configurer created with token endpoint");
            OSMLogger.logPerformance(this.getClass(), "getOAuth2AuthorizationServerConfigurer", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "OAUTH2_CONFIGURER_CREATED", 
                "OAuth2 Authorization Server configurer created with custom authentication providers");
            
            return configurer;
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error creating OAuth2 authorization server configurer", e);
            throw e;
        }
    }
}
