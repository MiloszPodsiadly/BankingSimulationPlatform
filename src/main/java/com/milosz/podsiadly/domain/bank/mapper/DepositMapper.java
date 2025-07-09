package com.milosz.podsiadly.domain.bank.mapper;

import com.milosz.podsiadly.domain.bank.dto.DepositDto;
import com.milosz.podsiadly.domain.bank.model.Deposit;
import com.milosz.podsiadly.domain.bank.model.BankAccount; // Potrzebne do mapowania konta
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface DepositMapper {

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "account.accountNumber", target = "accountNumber")
    DepositDto toDto(Deposit deposit);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "depositNumber", ignore = true) // Generowane w serwisie
    @Mapping(target = "status", ignore = true) // Ustawiane w serwisie
    @Mapping(target = "startDate", ignore = true) // Ustawiane w serwisie
    @Mapping(target = "endDate", ignore = true) // Ustawiane w serwisie
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "account", ignore = true) // Konto będzie ustawione ręcznie w serwisie
    Deposit toEntity(DepositDto depositDto); // Dto do tworzenia/aktualizacji depozytu
}
