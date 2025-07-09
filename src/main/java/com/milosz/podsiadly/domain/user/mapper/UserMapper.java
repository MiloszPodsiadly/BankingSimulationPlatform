package com.milosz.podsiadly.domain.user.mapper;

import com.milosz.podsiadly.domain.user.dto.RegisterRequest;
import com.milosz.podsiadly.domain.user.dto.UserDto;
import com.milosz.podsiadly.domain.user.model.Role;
import com.milosz.podsiadly.domain.user.model.User;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    // Mapowanie z encji User na UserDto
    @Mapping(source = "roles", target = "roles", qualifiedByName = "mapRolesToUserRoles")
    UserDto toUserDto(User user);
    List<UserDto> toUserDtoList(List<User> users);

    // Mapowanie z RegisterRequest na encję User (dla tworzenia)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true) // Hasło będzie hashowane w serwisie
    @Mapping(target = "registrationDate", ignore = true)
    @Mapping(target = "lastLoginDate", ignore = true)
    @Mapping(target = "bankAccounts", ignore = true)
    @Mapping(target = "active", constant = "true") // Domyślnie aktywny przy rejestracji
    @Mapping(target = "roles", ignore = true) // Roles will be set in service
    User toUserEntity(RegisterRequest registerRequest);

    // Mapowanie z RegisterRequest na istniejącą encję User (dla aktualizacji)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true) // Nazwa użytkownika zazwyczaj niezmienna
    @Mapping(target = "passwordHash", ignore = true) // Hasło aktualizowane oddzielnie
    @Mapping(target = "registrationDate", ignore = true)
    @Mapping(target = "lastLoginDate", ignore = true)
    @Mapping(target = "bankAccounts", ignore = true)
    @Mapping(target = "roles", ignore = true) // Roles updated separately
    void updateUserEntityFromRequest(RegisterRequest registerRequest, @MappingTarget User user);

    @Named("mapRolesToUserRoles")
    default Set<Role.UserRole> mapRolesToUserRoles(Set<Role> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}