package com.milosz.podsiadly.domain.user.repository;

import com.milosz.podsiadly.domain.user.model.Role;
import com.milosz.podsiadly.domain.user.model.Role.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(UserRole name);
}