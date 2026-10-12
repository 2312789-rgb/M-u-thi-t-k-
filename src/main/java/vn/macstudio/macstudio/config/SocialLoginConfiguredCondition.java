package vn.macstudio.macstudio.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class SocialLoginConfiguredCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var environment = context.getEnvironment();
        return hasAnyValue(environment.getProperty("GOOGLE_CLIENT_ID"))
            || hasAnyValue(environment.getProperty("GOOGLE_CLIENT_SECRET"))
            || hasAnyValue(environment.getProperty("APPLE_CLIENT_ID"))
            || hasAnyValue(environment.getProperty("APPLE_CLIENT_SECRET"));
    }

    private boolean hasAnyValue(String value) {
        return value != null && !value.isBlank();
    }
}
