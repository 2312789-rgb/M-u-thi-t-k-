package vn.macstudio.macstudio.service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SocialLoginSettings {
    private final boolean googleEnabled;
    private final boolean appleEnabled;

    public SocialLoginSettings(Environment environment) {
        googleEnabled = isConfigured(environment, "GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET");
        appleEnabled = isConfigured(environment, "APPLE_CLIENT_ID", "APPLE_CLIENT_SECRET");
    }

    public boolean isGoogleEnabled() { return googleEnabled; }
    public boolean isAppleEnabled() { return appleEnabled; }

    public static boolean isConfigured(Environment environment, String clientIdKey, String clientSecretKey) {
        return hasText(environment.getProperty(clientIdKey)) && hasText(environment.getProperty(clientSecretKey));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
