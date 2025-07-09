package com.milosz.podsiadly.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // Do zabezpieczania metod
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService; // Będzie potrzebne
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Jeśli będziesz dodawać filtr JWT

@Configuration
@EnableWebSecurity // Włącza wsparcie dla bezpieczeństwa webowego Springa
@EnableMethodSecurity // Włącza zabezpieczenia na poziomie metod (np. @PreAuthorize)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService; // Wstrzykujemy własny UserDetailsService
    // private final JwtAuthEntryPoint unauthorizedHandler; // Jeśli potrzebujesz obsługi błędów 401
    // private final JwtAuthFilter jwtAuthFilter; // Jeśli będziesz mieć filtr JWT

    // 1. Bean PasswordEncoder - już o tym rozmawialiśmy
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. Bean AuthenticationManager
    // To jest kluczowy element rozwiązujący Twój problem
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /*
    // Alternatywna konfiguracja AuthenticationManager, jeśli nie używasz AuthenticationConfiguration:
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService); // Wstrzyknięcie własnego UserDetailsService
        authenticationProvider.setPasswordEncoder(passwordEncoder()); // Użycie zdefiniowanego PasswordEncoder
        return new ProviderManager(authenticationProvider);
    }
    */

    // 3. Konfiguracja SecurityFilterChain (zasady autoryzacji URL-i)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Wyłącz CSRF dla API REST
                // .exceptionHandling(exceptions -> exceptions
                //     .authenticationEntryPoint(unauthorizedHandler) // Obsługa błędów 401
                // )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Sesje bezstanowe dla JWT
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**").permitAll() // Dostępne dla wszystkich (rejestracja, logowanie)
                        // Przykład: wymaga roli ADMIN lub EMPLOYEE dla /api/users
                        .requestMatchers("/api/users/**").hasAnyAuthority("ADMIN", "EMPLOYEE")
                        // Przykład: wymaga roli ADMIN dla usuwania użytkowników
                        .requestMatchers("/api/users/delete/**").hasAuthority("ADMIN")
                        // Dostęp do endpointów symulacji tylko dla zalogowanych użytkowników z odpowiednimi rolami
                        .requestMatchers("/api/simulations/**").hasAnyAuthority("ADMIN", "ANALYST", "SIMULATION_USER")
                        // Pozostałe endpointy wymagają uwierzytelnienia
                        .anyRequest().authenticated()
                );

        // Jeśli masz filtr JWT, dodaj go przed UsernamePasswordAuthenticationFilter
        // http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}