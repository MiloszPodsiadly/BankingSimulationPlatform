package com.milosz.podsiadly.common.security;
import com.milosz.podsiadly.domain.user.model.User;
import com.milosz.podsiadly.domain.user.repository.UserRepository;
import com.milosz.podsiadly.common.security.principal.UserPrincipal; // Nowy import
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // Zwracamy UserPrincipal zamiast ogólnego User
        return UserPrincipal.create(user);
    }
}