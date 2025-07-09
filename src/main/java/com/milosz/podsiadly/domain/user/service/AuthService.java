package com.milosz.podsiadly.domain.user.service;

import com.milosz.podsiadly.domain.user.dto.LoginResponse;
import com.milosz.podsiadly.domain.user.dto.RegisterRequest;
import com.milosz.podsiadly.domain.user.model.Role;
import com.milosz.podsiadly.domain.user.model.Role.UserRole;
import com.milosz.podsiadly.domain.user.model.User;
import com.milosz.podsiadly.domain.user.repository.RoleRepository;
import com.milosz.podsiadly.domain.user.repository.UserRepository;
import com.milosz.podsiadly.common.security.jwt.JwtTokenProvider; // Będzie potrzebne
import com.milosz.podsiadly.common.exception.UserAlreadyExistsException; // Będzie potrzebne
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager; // Będzie potrzebne
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager; // From Spring Security
    private final JwtTokenProvider jwtTokenProvider; // Custom JWT provider

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username is already taken!");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .active(true) // Default to active
                .build();

        Set<Role> roles = new HashSet<>();
        if (request.roles() == null || request.roles().isEmpty()) {
            // Default role if none specified
            Role customerRole = roleRepository.findByName(UserRole.CUSTOMER)
                    .orElseThrow(() -> new RuntimeException("Error: Role 'CUSTOMER' not found."));
            roles.add(customerRole);
        } else {
            request.roles().forEach(roleName -> {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Error: Role '" + roleName + "' not found."));
                roles.add(role);
            });
        }
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        log.info("User registered: {}", savedUser.getUsername());
        return savedUser;
    }

    @Transactional
    public LoginResponse authenticateUser(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found after authentication."));

        String jwt = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication); // Implement refresh token

        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user); // Update last login date

        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        log.info("User {} logged in successfully.", username);
        return new LoginResponse(jwt, refreshToken, user.getId(), user.getUsername(), roles);
    }

    // You might add refresh token logic here as well
}