package com.milosz.podsiadly.common.security.principal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.milosz.podsiadly.domain.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private Long id;
    private String username;
    @JsonIgnore // Nie eksponuj hasła w serializacji
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private boolean active;

    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isActive(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Domyślnie zawsze aktywne, jeśli nie ma logiki wygasania konta
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Domyślnie nigdy zablokowane
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Domyślnie nigdy wygasłe
    }

    @Override
    public boolean isEnabled() {
        return active; // Zależy od pola 'active' w encji User
    }

    // Ważne do poprawnego porównywania obiektów UserPrincipal
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}