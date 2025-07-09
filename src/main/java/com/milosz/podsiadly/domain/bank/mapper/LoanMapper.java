package com.milosz.podsiadly.domain.bank.mapper;

import com.milosz.podsiadly.domain.bank.dto.LoanApplicationDto;
import com.milosz.podsiadly.domain.bank.dto.LoanDto;
import com.milosz.podsiadly.domain.bank.model.Loan;
import com.milosz.podsiadly.domain.bank.model.BankAccount; // Potrzebne do mapowania konta
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface LoanMapper {

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "account.accountNumber", target = "accountNumber")
    LoanDto toDto(Loan loan);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "loanNumber", ignore = true) // Generowane w serwisie
    @Mapping(target = "status", ignore = true) // Ustawiane w serwisie
    @Mapping(target = "outstandingBalance", ignore = true) // Ustawiane w serwisie
    @Mapping(target = "startDate", ignore = true) // Ustawiane w serwisie
    @Mapping(target = "endDate", ignore = true) // Ustawiane w serwisie
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "account", ignore = true) // Konto będzie ustawione ręcznie w serwisie
    Loan toEntity(LoanApplicationDto loanApplicationDto);

    // Metoda do konwersji DTO odpowiedzi na encję, jeśli potrzebne (mniej typowe)
    // @Mapping(target = "account", ignore = true)
    // @Mapping(target = "createdAt", ignore = true)
    // @Mapping(target = "updatedAt", ignore = true)
    // Loan toEntity(LoanDto loanDto);
}
