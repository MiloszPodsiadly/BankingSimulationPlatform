package com.milosz.podsiadly.common.security;

import com.milosz.podsiadly.domain.user.model.User; // Importuj swój model User
import com.milosz.podsiadly.domain.user.repository.UserRepository; // Importuj swoje repozytorium User
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Potrzebne do ról

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // Konwertujemy role użytkownika na Spring Security GrantedAuthority
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(), // Upewnij się, że to jest zahashowane hasło!
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName().name())) // Mapujemy nazwy ról enum na String
                        .collect(Collectors.toSet())
        );
    }
}