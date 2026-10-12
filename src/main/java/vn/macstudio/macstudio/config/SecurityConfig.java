package vn.macstudio.macstudio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.ObjectProvider;
import vn.macstudio.macstudio.service.SocialLoginSettings;
import vn.macstudio.macstudio.service.StoreUserDetailsService;
import vn.macstudio.macstudio.web.OAuthLoginSuccessHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, StoreUserDetailsService users,
                                             ObjectProvider<ClientRegistrationRepository> registrations,
                                             SocialLoginSettings socialLogin, OAuthLoginSuccessHandler successHandler) throws Exception {
        http
            .userDetailsService(users)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/products/**", "/account/login", "/account/register", "/css/**", "/js/**", "/images/**", "/uploads/**", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/chat/**").authenticated()
                .requestMatchers("/notifications/**").authenticated()
                .requestMatchers("/account/profile").authenticated()
                .requestMatchers("/orders/**", "/cart/checkout", "/cart/confirmation/**").hasRole("CUSTOMER")
                .anyRequest().permitAll())
            .formLogin(form -> form.loginPage("/account/login").usernameParameter("email").defaultSuccessUrl("/", false).permitAll())
            .logout(logout -> logout.logoutUrl("/account/logout").logoutSuccessUrl("/"));

        ClientRegistrationRepository repository = registrations.getIfAvailable();
        if (repository != null) {
            http.oauth2Login(oauth -> oauth
                .clientRegistrationRepository(repository)
                .authorizationEndpoint(endpoint ->
                    endpoint.authorizationRequestResolver(authorizationRequestResolver(repository)))
                .successHandler(successHandler)
                .failureUrl("/account/login?oauthError"));
            if (socialLogin.isAppleEnabled()) {
                http.csrf(csrf -> csrf.ignoringRequestMatchers(request ->
                    "POST".equals(request.getMethod())
                        && "/login/oauth2/code/apple".equals(request.getServletPath())));
            }
        }
        return http.build();
    }

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(ClientRegistrationRepository registrations) {
        var delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest resolve(
                jakarta.servlet.http.HttpServletRequest request) {
                var requestAuthorization = delegate.resolve(request);
                boolean apple = request.getRequestURI().endsWith("/oauth2/authorization/apple");
                return addAppleResponseMode(requestAuthorization, apple);
            }

            @Override
            public org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest resolve(
                jakarta.servlet.http.HttpServletRequest request, String clientRegistrationId) {
                return addAppleResponseMode(delegate.resolve(request, clientRegistrationId), "apple".equals(clientRegistrationId));
            }

            private org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest addAppleResponseMode(
                org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest authorizationRequest,
                boolean apple) {
                if (!apple || authorizationRequest == null) return authorizationRequest;
                return org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest.from(authorizationRequest)
                    .additionalParameters(parameters -> parameters.put("response_mode", "form_post"))
                    .build();
            }
        };
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}