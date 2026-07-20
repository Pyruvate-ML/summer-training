package com.xinqiao.counseling;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "DOCTOR", "PATIENT")
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
            User.withUsername("admin")
                .password("{noop}admin123")
                .roles("ADMIN")
                .build(),
            User.withUsername("doctor_zhang")
                .password("{noop}doctor123")
                .roles("DOCTOR")
                .build(),
            User.withUsername("doctor_lin")
                .password("{noop}doctor123")
                .roles("DOCTOR")
                .build(),
            User.withUsername("patient_chen")
                .password("{noop}patient123")
                .roles("PATIENT")
                .build(),
            User.withUsername("patient_li")
                .password("{noop}patient123")
                .roles("PATIENT")
                .build()
        );
    }
}
