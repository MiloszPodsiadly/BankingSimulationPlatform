package com.milosz.podsiadly.domain.bank.mapper;

import com.milosz.podsiadly.domain.bank.dto.AccountDto;
import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.model.Bank; // Potrzebne do mapowania banku
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring", uses = {BankMapper.class}) // MapStruct użyje BankMapper, jeśli będzie potrzebny
@Component
public interface AccountMapper {

    @Mapping(source = "bank.id", target = "bankId")
    @Mapping(source = "bank.name", target = "bankName")
    AccountDto toDto(BankAccount bankAccount);

    @Mapping(target = "bank", ignore = true) // Bank będzie ustawiony ręcznie w serwisie
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BankAccount toEntity(AccountDto accountDto);

    // Jeśli chcesz aktualizować encję z DTO
    // @Mapping(target = "id", ignore = true)
    // @Mapping(target = "bank", ignore = true)
    // @Mapping(target = "createdAt", ignore = true)
    // @Mapping(target = "updatedAt", ignore = true)
    // void updateAccountFromDto(AccountDto accountDto, @MappingTarget BankAccount bankAccount);
}
