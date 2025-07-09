package com.milosz.podsiadly.domain.bank.mapper;

import com.milosz.podsiadly.domain.bank.dto.TransactionDto;
import com.milosz.podsiadly.domain.bank.dto.TransactionRequest;
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.bank.model.BankAccount; // Potrzebne do mapowania konta
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface TransactionMapper {

    @Mapping(source = "sourceAccount.id", target = "sourceAccountId")
    @Mapping(source = "sourceAccount.accountNumber", target = "sourceAccountNumber")
    @Mapping(source = "targetAccount.id", target = "targetAccountId")
    @Mapping(source = "targetAccount.accountNumber", target = "targetAccountNumber")
    TransactionDto toDto(Transaction transaction);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transactionRef", ignore = true) // Generowane w serwisie
    @Mapping(target = "status", ignore = true) // Ustawiane w serwisie
    @Mapping(target = "transactionDate", ignore = true) // Ustawiane w serwisie
    @Mapping(target = "sourceAccount", ignore = true) // Ustawiane w serwisie
    @Mapping(target = "targetAccount", ignore = true) // Ustawiane w serwisie
    @Mapping(target = "type", expression = "java(com.milosz.podsiadly.domain.bank.model.Transaction.TransactionType.TRANSFER)") // Domyślny typ dla requestu
    Transaction toEntity(TransactionRequest transactionRequest);
}
