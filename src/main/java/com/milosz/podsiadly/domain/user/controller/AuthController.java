package com.milosz.podsiadly.domain.user.controller;

import com.milosz.podsiadly.domain.user.dto.LoginResponse;
import com.milosz.podsiadly.domain.user.dto.RegisterRequest;
import com.milosz.podsiadly.domain.user.dto.UserDto;
import com.milosz.podsiadly.domain.user.mapper.UserMapper;
import com.milosz.podsiadly.domain.user.model.User;
import com.milosz.podsiadly.domain.user.service.AuthService;
import com.milosz.podsiadly.common.exception.UserAlreadyExistsException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper; // To map User entity to UserDto for registration response

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@Valid @RequestBody RegisterRequest request) {
        log.info("Attempting to register new user: {}", request.username());
        try {
            User registeredUser = authService.registerUser(request);
            return new ResponseEntity<>(userMapper.toUserDto(registeredUser), HttpStatus.CREATED);
        } catch (UserAlreadyExistsException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@Valid @RequestBody com.milosz.podsiadly.domain.user.dto.UserLoginRequest loginRequest) {
        log.info("Attempting to authenticate user: {}", loginRequest.username());
        try {
            LoginResponse response = authService.authenticateUser(loginRequest.username(), loginRequest.password());
            return ResponseEntity.ok(response);
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("Authentication failed for user {}: {}", loginRequest.username(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        } catch (Exception e) {
            log.error("Error during user authentication: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // You might add a /refresh-token endpoint here
}