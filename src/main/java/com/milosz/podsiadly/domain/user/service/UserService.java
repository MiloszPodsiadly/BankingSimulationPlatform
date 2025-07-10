package com.milosz.podsiadly.domain.user.service;

import com.milosz.podsiadly.domain.user.model.Role;
import com.milosz.podsiadly.domain.user.model.Role.UserRole;
import com.milosz.podsiadly.domain.user.model.User;
import com.milosz.podsiadly.domain.user.repository.RoleRepository;
import com.milosz.podsiadly.domain.user.repository.UserRepository;
import com.milosz.podsiadly.common.exception.ResourceNotFoundException;
import com.milosz.podsiadly.common.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new user (for admin purposes or internal use where roles are explicitly set).
     * @param username The username.
     * @param email The email.
     * @param password The raw password (will be hashed).
     * @param firstName The first name.
     * @param lastName The last name.
     * @param roleNames The set of user roles.
     * @param active Initial active status.
     * @return The created User entity.
     * @throws UserAlreadyExistsException if username or email already exists.
     * @throws ResourceNotFoundException if a specified role does not exist.
     */
    @Transactional
    public User createUser(String username, String email, String password, String firstName, String lastName, Set<UserRole> roleNames, boolean active) {
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("User with username '" + username + "' already exists.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User with email '" + email + "' already exists.");
        }

        Set<Role> roles = new HashSet<>();
        if (roleNames != null && !roleNames.isEmpty()) {
            roleNames.forEach(roleName -> {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role '" + roleName.name() + "' not found."));
                roles.add(role);
            });
        } else {
            // Default role if no roles are provided (e.g., for direct admin creation)
            Role customerRole = roleRepository.findByName(UserRole.CUSTOMER)
                    .orElseThrow(() -> new ResourceNotFoundException("Role 'CUSTOMER' not found."));
            roles.add(customerRole);
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .roles(roles)
                .active(active)
                .build();
        User savedUser = userRepository.save(user);
        log.info("User created: {}", savedUser.getUsername());
        return savedUser;
    }

    /**
     * Retrieves a user by ID.
     * @param id The user ID.
     * @return An Optional containing the User entity.
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Retrieves a user by username.
     * @param username The username.
     * @return An Optional containing the User entity.
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Retrieves all users.
     * @return A list of all User entities.
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Updates an existing user's details (excluding password and username).
     * @param id The ID of the user to update.
     * @param email The new email.
     * @param firstName The new first name.
     * @param lastName The new last name.
     * @param roleNames The new set of roles.
     * @param active The new active status.
     * @return The updated User entity.
     * @throws ResourceNotFoundException if user or a specified role not found.
     */
    @Transactional
    public User updateUser(Long id, String email, String firstName, String lastName, Set<UserRole> roleNames, boolean active) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (!existingUser.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email '" + email + "' is already in use by another user.");
        }

        Set<Role> updatedRoles = new HashSet<>();
        if (roleNames != null && !roleNames.isEmpty()) {
            for (UserRole roleName : roleNames) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role '" + roleName.name() + "' not found."));
                updatedRoles.add(role);
            }

    } else {
            // Keep existing roles if none provided for update, or set a default if desired
            log.warn("No roles provided for user update. Keeping existing roles for user ID: {}", id);
            updatedRoles = existingUser.getRoles(); // Or assign a default if null/empty roles are allowed for update
        }

        existingUser.setEmail(email);
        existingUser.setFirstName(firstName);
        existingUser.setLastName(lastName);
        existingUser.setRoles(updatedRoles);
        existingUser.setActive(active);

        User savedUser = userRepository.save(existingUser);
        log.info("User updated: {}", savedUser.getUsername());
        return savedUser;
    }

    /**
     * Updates only user's password.
     * @param id The ID of the user.
     * @param newPassword The new raw password.
     * @return The updated User entity.
     * @throws ResourceNotFoundException if user not found.
     */
    @Transactional
    public User updatePassword(Long id, String newPassword) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        existingUser.setPasswordHash(passwordEncoder.encode(newPassword));
        User savedUser = userRepository.save(existingUser);
        log.info("User password updated for user ID: {}", id);
        return savedUser;
    }

    /**
     * Deactivates a user account.
     * @param id The ID of the user to deactivate.
     * @return The deactivated User entity.
     * @throws ResourceNotFoundException if user not found.
     */
    @Transactional
    public User deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        user.setActive(false);
        User deactivatedUser = userRepository.save(user);
        log.info("User {} deactivated.", deactivatedUser.getUsername());
        return deactivatedUser;
    }

    /**
     * Activates a user account.
     * @param id The ID of the user to activate.
     * @return The activated User entity.
     * @throws ResourceNotFoundException if user not found.
     */
    @Transactional
    public User activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        user.setActive(true);
        User activatedUser = userRepository.save(user);
        log.info("User {} activated.", activatedUser.getUsername());
        return activatedUser;
    }

    /**
     * Deletes a user by ID.
     * @param id The user ID.
     * @throws ResourceNotFoundException if user not found.
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
        log.info("User with ID {} deleted.", id);
    }

    /**
     * Updates user's last login date.
     * @param username The username of the user.
     */
    @Transactional
    public void updateLastLoginDate(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLoginDate(LocalDateTime.now());
            userRepository.save(user);
            log.debug("Updated last login date for user: {}", username);
        });
    }

    // Metoda dla ScenarioGenerator do tworzenia użytkownika symulacyjnego
    @Transactional
    public User createSimulationUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            log.warn("Attempted to create duplicate user/email during simulation: {}", username);
            return userRepository.findByUsername(username).orElse(null);
        }

        Role simulationRole = roleRepository.findByName(UserRole.SIMULATION_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role 'SIMULATION_USER' not found. Please ensure it exists."));
        Set<Role> roles = new HashSet<>();
        roles.add(simulationRole);

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .email(email)
                .firstName("Simulated")
                .lastName("User")
                .roles(roles)
                .active(true)
                .build();
        User savedUser = userRepository.save(user);
        log.info("Simulated user created: {}", savedUser.getUsername());
        return savedUser;
    }
}