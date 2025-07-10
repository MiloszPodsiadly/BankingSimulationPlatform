package com.milosz.podsiadly.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents an event indicating that a new user has been successfully registered.
 * This event is crucial for various downstream processes such as sending welcome emails,
 * updating user statistics, or initial risk assessment for new users.
 */
@Data // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
@Builder // Provides a builder pattern for easy object creation
public class UserRegisteredEvent {

    /**
     * Unique identifier of the newly registered user.
     */
    private Long userId;

    /**
     * The username of the newly registered user.
     */
    private String username;

    /**
     * The email address of the newly registered user.
     */
    private String email;

    /**
     * Timestamp when the user was registered (and when this event was generated).
     */
    private LocalDateTime registeredAt;

    // Możesz dodać więcej pól, jeśli są kluczowe dla tego zdarzenia,
    // np. role użytkownika, dane kontaktowe, itp.
}
