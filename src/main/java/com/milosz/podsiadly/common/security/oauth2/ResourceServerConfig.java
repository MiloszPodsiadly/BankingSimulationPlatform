package com.milosz.podsiadly.common.security.oauth2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Klasa konfiguracyjna Spring Security dla Resource Servera OAuth2.
 * Definiuje, które ścieżki URL wymagają autoryzacji opartej na tokenie JWT
 * oraz konfiguruje dekoder tokenów JWT.
 */
@Configuration // Adnotacja oznacza, że ta klasa zawiera definicje beanów i jest źródłem konfiguracji Springa.
@EnableWebSecurity // Włącza wsparcie dla bezpieczeństwa sieciowego Spring Security.
public class ResourceServerConfig {

    /**
     * Definiuje łańcuch filtrów bezpieczeństwa (SecurityFilterChain) dla aplikacji.
     * To jest serce konfiguracji Spring Security, gdzie określamy, jakie żądania
     * są autoryzowane, a jakie nie.
     *
     * @param http Obiekt HttpSecurity, używany do konfiguracji zabezpieczeń HTTP.
     * @return Skonfigurowany SecurityFilterChain.
     * @throws Exception Jeśli wystąpi błąd podczas konfiguracji.
     */
    /*
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Wyłącza ochronę CSRF (Cross-Site Request Forgery). Jest to często wymagane
                // w aplikacjach typu REST API, które nie używają sesji i opierają się na tokenach.
                // W przeciwnym razie każde żądanie inne niż GET musiałoby zawierać token CSRF.
                .csrf(csrf -> csrf.disable())

                // Konfiguruje autoryzację żądań HTTP.
                .authorizeHttpRequests(authorize -> authorize
                        // Pozwala na dostęp do ścieżki /actuator/** bez autoryzacji.
                        // Jest to przydatne do monitorowania stanu aplikacji, metryk itp.
                        // (pamiętaj, aby zabezpieczyć te endpointy w produkcji, np. tylko dla konkretnych IP).
                        .requestMatchers("/actuator/**").permitAll()
                        // Pozwala na dostęp do ścieżek związanych z dokumentacją OpenAPI (Swagger UI) bez autoryzacji.
                        // Ułatwia to testowanie i przeglądanie API.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Pozwala na dostęp do endpointów uwierzytelniania/rejestracji (jeśli takie posiadasz)
                        // W przypadku autoryzacji na podstawie JWT, to tutaj będziesz mieć logikę generowania tokenów.
                        // Przykład: .requestMatchers("/api/auth/**").permitAll()
                        // Wszystkie inne żądania wymagają uwierzytelnienia.
                        .anyRequest().authenticated()
                )
                // Konfiguruje Resource Servera OAuth2, aby używał tokenów JWT.
                // Spring Security automatycznie próbuje zdekodować i zweryfikować token JWT
                // obecny w nagłówku "Authorization: Bearer <token>".
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults())); // Używa domyślnego dekodera JWT.

        return http.build(); // Buduje i zwraca skonfigurowany SecurityFilterChain.
    }
    */


    /**
     * Konfiguruje dekoder tokenów JWT.
     * Ta metoda jest kluczowa, ponieważ informuje Spring Security, skąd pobierać
     * klucze publiczne (lub inne dane) potrzebne do weryfikacji podpisu tokena JWT.
     *
     * @return Skonfigurowany JwtDecoder.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // WAŻNE: Adres URL poniżej musi wskazywać na endpoint JWKS (JSON Web Key Set)
        // Twojego serwera autoryzacji (Identity Providera), np. Keycloak, Auth0, Okta.
        // Spring Security pobierze z niego klucz publiczny do weryfikacji tokenów.
        // Jeśli generujesz tokeny samodzielnie, musisz dostarczyć odpowiedni klucz publiczny.
        // Przykład dla Keycloak:
        // return NimbusJwtDecoder.withJwkSetUri("http://localhost:8080/realms/your_realm/protocol/openid-connect/certs").build();

        // Poniżej znajduje się PRZYKŁADOWA konfiguracja, jeśli masz statyczny klucz publiczny
        // (np. certyfikat, który eksportujesz z serwera autoryzacji).
        // W PRZYPADKU PRODUKCJI, ZAWSZE UŻYWAJ JWKS URI.
        // DO CELÓW TESTOWYCH/DEVELOPERSKICH MOŻESZ POTRZEBOWAĆ CZEGOŚ TAKIEGO:
        // final String secret = "your-very-secret-key-that-is-at-least-256-bits-long-and-should-be-kept-safe";
        // SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSha256");
        // return NimbusJwtDecoder.withSecretKey(secretKey).build();

        // Najbardziej typowym i zalecanym podejściem jest JWK Set URI:
        // W pliku application.properties lub application.yml ustawiasz:
        // spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/realms/your_realm/protocol/openid-connect/certs
        // A potem po prostu używasz domyślnego mechanizmu:
        return NimbusJwtDecoder.withJwkSetUri("http://localhost:8080/realms/master/protocol/openid-connect/certs").build();
        // ZASTĄP "http://localhost:8080/realms/master/protocol/openid-connect/certs" na właściwy adres JWKS Twojego Identity Providera.
    }
}
