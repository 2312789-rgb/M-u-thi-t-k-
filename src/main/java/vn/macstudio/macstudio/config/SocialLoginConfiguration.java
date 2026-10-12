package vn.macstudio.macstudio.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
public class SocialLoginConfiguration {
    @Bean
    @Conditional(SocialLoginConfiguredCondition.class)
    ClientRegistrationRepository clientRegistrationRepository(Environment environment) {
        List<ClientRegistration> registrations = new ArrayList<>();
        addGoogleRegistration(environment, registrations);
        addAppleRegistration(environment, registrations);
        return new InMemoryClientRegistrationRepository(registrations);
    }

    private void addGoogleRegistration(Environment environment, List<ClientRegistration> registrations) {
        String clientId = environment.getProperty("GOOGLE_CLIENT_ID");
        String clientSecret = environment.getProperty("GOOGLE_CLIENT_SECRET");
        if (isPartialConfiguration(clientId, clientSecret))
            throw new IllegalStateException("Cần cấu hình cả GOOGLE_CLIENT_ID và GOOGLE_CLIENT_SECRET.");
        if (clientId == null || clientId.isBlank()) return;
        registrations.add(ClientRegistrations.fromIssuerLocation("https://accounts.google.com")
            .registrationId("google")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .clientName("Google")
            .build());
    }

    private void addAppleRegistration(Environment environment, List<ClientRegistration> registrations) {
        String clientId = environment.getProperty("APPLE_CLIENT_ID");
        String clientSecret = environment.getProperty("APPLE_CLIENT_SECRET");
        if (isPartialConfiguration(clientId, clientSecret))
            throw new IllegalStateException("Cần cấu hình cả APPLE_CLIENT_ID và APPLE_CLIENT_SECRET.");
        if (clientId == null || clientId.isBlank()) return;
        registrations.add(ClientRegistrations.fromIssuerLocation("https://appleid.apple.com")
            .registrationId("apple")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "email", "name")
            .clientName("Apple")
            .build());
    }

    private boolean isPartialConfiguration(String clientId, String clientSecret) {
        boolean hasClientId = clientId != null && !clientId.isBlank();
        boolean hasClientSecret = clientSecret != null && !clientSecret.isBlank();
        return hasClientId != hasClientSecret;
    }
}
