package com.milosz.podsiadly.common.security.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity // Zapewnia podstawową konfigurację bezpieczeństwa webowego
@RequiredArgsConstructor
public class ResourceServerConfig {

    // Wstrzyknij sekret JWT z properties, używany do weryfikacji tokenów
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    // Możesz usunąć tę klasę, jeśli SecurityConfig już konfiguruje SecurityFilterChain
    // Jeśli jednak chcesz mieć oddzielną konfigurację dla OAuth2 Resource Server,
    // to ten bean jest odpowiedni. W tym przypadku, to SecurityConfig będzie głównym
    // miejscem konfiguracji HTTP Security.
    // Pamiętaj, aby nie mieć dwóch SecurityFilterChain Beanów włączonych jednocześnie
    // bez odpowiedniego porządku (@Order) lub warunków, bo Spring nie będzie wiedział,
    // którego użyć.

    // Ta konfiguracja zakłada, że masz oddzielną SecurityFilterChain dla reszty aplikacji
    // w SecurityConfig.
    // Jeśli jednak ten ResourceServerConfig ma być główną konfiguracją bezpieczeństwa,
    // to przeniesiesz do niego wszystkie authorizeHttpRequests().

    /*
    // PRZYKŁADOWA KONFIGURACJA DLA SERWERA ZASOBÓW (JEŚLI POTRZEBUJESZ ODDZIELNEJ)
    @Bean
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(new CustomJwtAuthenticationConverter()) // Użyj naszego konwertera
                )
            );
        return http.build();
    }
    */

    // Jest to jednak kluczowy element dla Resource Servera: JwtDecoder
    // Zapewnia on sposób na dekodowanie i weryfikację tokenów JWT.
    @Bean
    public JwtDecoder jwtDecoder() {
        // Klucz musi być tego samego typu i wartości, jak używany do podpisywania
        SecretKey secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}