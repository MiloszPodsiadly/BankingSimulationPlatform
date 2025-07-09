package com.milosz.podsiadly.common.security.oauth2;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Konwerter, który przekształca obiekt Jwt (zawierający roszczenia z tokenu JWT)
 * na obiekt JwtAuthenticationToken, który jest rozumiany przez Spring Security.
 * Odpowiada za mapowanie ról z tokenu JWT (np. z klucza "roles" lub "scope") na Spring Security GrantedAuthorities.
 */
@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Przykład: Role są przechowywane w claims jako lista stringów pod kluczem "roles"
        // Jeśli Keycloak używa "realm_access.roles", musisz dostosować ścieżkę
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        List<String> roles = (List<String>) realmAccess.get("roles");

        if (roles == null) {
            return List.of();
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())) // Dodaj prefiks ROLE_
                .collect(Collectors.toList());
    }
}