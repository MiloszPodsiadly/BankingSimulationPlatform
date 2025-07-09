package com.milosz.podsiadly.common.security.oauth2;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    // Domyślny konwerter Spring Security dla zakresów (scopes) JWT
    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                jwtGrantedAuthoritiesConverter.convert(jwt).stream(), // Standardowe role z 'scope' lub 'scp'
                extractRoles(jwt).stream() // Nasze niestandardowe role z 'roles' claim
        ).collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("sub")); // Używamy "sub" jako nazwy użytkownika
    }

    // Metoda do wyciągania ról z claimu "roles" (który dodaliśmy do naszego tokena JWT)
    private Collection<? extends GrantedAuthority> extractRoles(Jwt jwt) {
        // Zakładamy, że role są przechowywane w claimie "roles" jako lista stringów
        Object roles = jwt.getClaim("roles");
        if (roles instanceof Collection<?>) {
            return ((Collection<?>) roles).stream()
                    .map(Object::toString)
                    .map(role -> new SimpleGrantedAuthority(role)) // Mapujemy role jako SimpleGrantedAuthority
                    .collect(Collectors.toSet());
        }
        return new HashSet<>(); // Zwróć pusty zbiór, jeśli brak ról
    }
}