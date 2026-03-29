package com.brownieshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * We use manual session-based auth (not Spring Security's form login).
 * This config simply opens all routes – our controllers handle auth checks themselves.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())          // disable CSRF for simplicity (re-enable for prod)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()          // session guards are in controllers
                )
                .formLogin(form -> form.disable())     // we use our own login pages
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}