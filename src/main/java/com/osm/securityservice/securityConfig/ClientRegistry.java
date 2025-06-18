package com.osm.securityservice.securityConfig;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.UUID;

@Configuration
public class ClientRegistry {
    private final PasswordEncoder passwordEncoder;
    @Value("${oauth2.client.id}")
    private String clientId;
    @Value("${oauth2.client.secret}")
    private String clientSecret;

    public ClientRegistry(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

//    @Bean
//    public RegisteredClientRepository registeredClientRepository() {
//        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
//                .clientId(clientId)
//                .clientSecret(passwordEncoder.encode(clientSecret))
//                .scope(OidcScopes.OPENID)
//                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//                .authorizationGrantType(new AuthorizationGrantType("TOKEN"))
//                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
//                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
//                .authorizationGrantType(AuthorizationGrantType.JWT_BEARER)
//                .tokenSettings(TokenSettings.builder()
//                        .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
//                        .accessTokenTimeToLive(Duration.ofMinutes(5))
//                        .refreshTokenTimeToLive(Duration.ofHours(1))
//                        .reuseRefreshTokens(false)
//                        .build())
//                .scope("read")
//                .scope("write")
//                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
//                .build();
//        return new InMemoryRegisteredClientRepository(client);
//    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);

        // Check if client exists already (avoid duplicates on restarts)
        if (repository.findByClientId(clientId) == null) {
            RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientSecret(passwordEncoder.encode(clientSecret))
                    .scope(OidcScopes.OPENID)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(new AuthorizationGrantType("TOKEN"))
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.JWT_BEARER)
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                            .accessTokenTimeToLive(Duration.ofMinutes(5))
                            .refreshTokenTimeToLive(Duration.ofHours(24))
                            .reuseRefreshTokens(false)
                            .build())
                    .scope("read")
                    .scope("write")
                    .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                    .build();

            repository.save(client);
        }

        return repository;
    }

}
