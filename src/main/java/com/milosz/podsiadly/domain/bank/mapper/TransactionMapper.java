package com.milosz.podsiadly.domain.bank.mapper;

import com.milosz.podsiadly.domain.bank.dto.TransactionDto;
import com.milosz.podsiadly.domain.bank.dto.TransactionRequest;
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.bank.model.BankAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy; // Import dla ReportingPolicy

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {

    // Mapowanie z encji Transaction na TransactionDto
    @Mapping(source = "sourceAccount.id", target = "sourceAccountId")
    @Mapping(source = "sourceAccount.accountNumber", target = "sourceAccountNumber")
    @Mapping(source = "targetAccount.id", target = "targetAccountId")
    @Mapping(source = "targetAccount.accountNumber", target = "targetAccountNumber")
    // W encji Transaction pola to 'type' i 'status' (enumy)
    // W DTO pola to 'type' i 'status' (enumy), więc nazwy są zgodne i MapStruct zmapuje je automatycznie
    // @Mapping(source = "type", target = "type") // Ta linia nie jest potrzebna, jeśli nazwy są takie same
    // @Mapping(source = "status", target = "status") // Ta linia nie jest potrzebna, jeśli nazwy są takie same
    TransactionDto toDto(Transaction transaction);

    List<TransactionDto> toDtoList(List<Transaction> transactions);


    // Mapowanie z TransactionRequest na encję Transaction
    // Pola, które będą ustawiane w serwisie lub przez bazę danych, muszą być ignorowane
    @Mapping(target = "id", ignore = true) // ID generowane przez bazę danych
    @Mapping(target = "transactionRef", ignore = true) // Generowane w serwisie
    @Mapping(target = "transactionDate", ignore = true) // Ustawiane w @PrePersist lub w serwisie
    @Mapping(target = "status", ignore = true) // Ustawiane w serwisie (np. PENDING)
    @Mapping(target = "type", ignore = true) // Ustawiane w serwisie na podstawie kontekstu transakcji (np. TRANSFER)

    // fromAccount i toAccount to obiekty BankAccount, które musimy pobrać z repozytorium w serwisie
    // Nie są one dostępne bezpośrednio w TransactionRequest
    @Mapping(target = "sourceAccount", ignore = true)
    @Mapping(target = "targetAccount", ignore = true)

    // Pola mapowane bezpośrednio z TransactionRequest na encję Transaction
    // Upewnij się, że nazwy pól są zgodne w obu klasach
    @Mapping(source = "amount", target = "amount")
    @Mapping(source = "currency", target = "currency")
    @Mapping(source = "description", target = "description")
    Transaction toEntity(TransactionRequest transactionRequest);
}