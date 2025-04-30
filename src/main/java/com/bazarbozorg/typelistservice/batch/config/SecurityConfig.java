package com.bazarbozorg.typelistservice.batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${swagger.username:admin}")
    private String swaggerUsername;

    @Value("${swagger.password:password}")
    private String swaggerPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails user = User.builder()
                .username(swaggerUsername)
                .password(passwordEncoder().encode(swaggerPassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        // Secure Swagger UI and API docs
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").authenticated()
                        // Allow batch job trigger endpoints to be used without authentication
                        .requestMatchers("/run-import-job", "/run-parallel-job", "/run-partitioned-job").permitAll()
                        // Allow actuator endpoints for monitoring
                        .requestMatchers("/actuator/**").permitAll()
                        // Secure everything else
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults())
                .csrf(csrf -> csrf.disable());  // Disable CSRF protection for simplicity (enable in production with proper tokens)

        return http.build();
    }
}