package com.uestcfir.config;

import com.uestcfir.filter.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter sessionFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/user/login", "/user/login/**", "/user/register", "/user/sendcode",
                                "/user/changepassword", "/approver/login", "/approver/login/**",
                                "/approver/register", "/approver/sendcode", "/swagger-ui/**",
                                "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**",
                                "/", "/index.html", "/styles/**", "/scripts/**", "/assets/**",
                                "/vendor/**", "/iot/ws/device", "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Prevent Boot from registering the same authentication filter twice. */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> sessionFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
