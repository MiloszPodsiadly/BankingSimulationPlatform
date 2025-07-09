package com.milosz.podsiadly.domain.user.controller;

import com.milosz.podsiadly.domain.user.dto.RegisterRequest; // Reusing for update
import com.milosz.podsiadly.domain.user.dto.UserDto;
import com.milosz.podsiadly.domain.user.mapper.UserMapper;
import com.milosz.podsiadly.domain.user.model.User;
import com.milosz.podsiadly.domain.user.service.UserService;
import com.milosz.podsiadly.common.exception.ResourceNotFoundException;
import com.milosz.podsiadly.common.exception.UserAlreadyExistsException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    // Typically, user creation is handled by AuthController.register.
    // This POST method might be for admin users creating new accounts directly.
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody RegisterRequest request) {
        log.info("Request to create user (admin/internal): {}", request.username());
        try {
            User createdUser = userService.createUser(
                    request.username(),
                    request.email(),
                    request.password(),
                    request.firstName(),
                    request.lastName(),
                    request.roles(),
                    request.active() // 'active' status can be set during creation
            );
            return new ResponseEntity<>(userMapper.toUserDto(createdUser), HttpStatus.CREATED);
        } catch (UserAlreadyExistsException e) {
            log.warn("User creation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (ResourceNotFoundException e) { // If a specified role does not exist
            log.warn("User creation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        log.info("Request to get user by ID: {}", id);
        return userService.getUserById(id)
                .map(userMapper::toUserDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        log.info("Request to get all users.");
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(userMapper.toUserDtoList(users));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody RegisterRequest request) {
        log.info("Request to update user ID: {}", id);
        try {
            User updatedUser = userService.updateUser(
                    id,
                    request.email(),
                    request.firstName(),
                    request.lastName(),
                    request.roles(), // Roles can be updated by admin
                    request.active()
            );
            return ResponseEntity.ok(userMapper.toUserDto(updatedUser));
        } catch (ResourceNotFoundException e) {
            log.warn("User update failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (UserAlreadyExistsException e) {
            log.warn("User update failed due to email conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error updating user ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<UserDto> deactivateUser(@PathVariable Long id) {
        log.info("Request to deactivate user ID: {}", id);
        try {
            User deactivatedUser = userService.deactivateUser(id);
            return ResponseEntity.ok(userMapper.toUserDto(deactivatedUser));
        } catch (ResourceNotFoundException e) {
            log.warn("User deactivation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<UserDto> activateUser(@PathVariable Long id) {
        log.info("Request to activate user ID: {}", id);
        try {
            User activatedUser = userService.activateUser(id);
            return ResponseEntity.ok(userMapper.toUserDto(activatedUser));
        } catch (ResourceNotFoundException e) {
            log.warn("User activation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Request to delete user ID: {}", id);
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("User deletion failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}