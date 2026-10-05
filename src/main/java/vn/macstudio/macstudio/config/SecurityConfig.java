package vn.macstudio.macstudio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import vn.macstudio.macstudio.service.StoreUserDetailsService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, StoreUserDetailsService users) throws Exception {
        return http
            .userDetailsService(users)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/products/**", "/account/login", "/account/register", "/css/**", "/js/**", "/images/**", "/uploads/**", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/notifications/**").authenticated()
                .requestMatchers("/account/profile").authenticated()
                .requestMatchers("/orders/**", "/cart/checkout", "/cart/confirmation/**").hasRole("CUSTOMER")
                .anyRequest().permitAll())
            .formLogin(form -> form.loginPage("/account/login").usernameParameter("email").defaultSuccessUrl("/", false).permitAll())
            .logout(logout -> logout.logoutUrl("/account/logout").logoutSuccessUrl("/"))
            .build();
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}