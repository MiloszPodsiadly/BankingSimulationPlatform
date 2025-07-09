package com.milosz.podsiadly.domain.bank.mapper;

import com.milosz.podsiadly.domain.bank.dto.TransactionDto;
import com.milosz.podsiadly.domain.bank.dto.TransactionRequest;
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.bank.model.BankAccount; // Wciąż potrzebne, jeśli MapStruct musi odwoływać się do BankAccount

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy; // Dodaj ten import

import java.util.List; // Dodaj ten import, jeśli mapujesz listy

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
// Usunięto `@Component` - `@Mapper(componentModel = "spring")` już sprawia, że jest komponentem Springa.
public interface TransactionMapper {

    // Mapowanie z encji Transaction na TransactionDto
    @Mapping(source = "fromAccount.id", target = "sourceAccountId")
    @Mapping(source = "fromAccount.accountNumber", target = "sourceAccountNumber")
    @Mapping(source = "toAccount.id", target = "targetAccountId")
    @Mapping(source = "toAccount.accountNumber", target = "targetAccountNumber")
    @Mapping(source = "transactionType", target = "type") // Mapowanie 'transactionType' z encji na 'type' w DTO
    @Mapping(source = "status", target = "status")       // Mapowanie 'status' z encji na 'status' w DTO
    TransactionDto toDto(Transaction transaction);

    // Dodajemy metodę do mapowania listy transakcji, która będzie potrzebna w ReportGenerator
    List<TransactionDto> toDtoList(List<Transaction> transactions);


    // Mapowanie z TransactionRequest na encję Transaction
    @Mapping(target = "id", ignore = true) // ID będzie generowane przez bazę danych
    @Mapping(target = "transactionRef", ignore = true) // transactionRef będzie generowane w serwisie
    @Mapping(target = "transactionDate", ignore = true) // transactionDate będzie ustawiane w serwisie (np. LocalDateTime.now())
    @Mapping(target = "status", ignore = true) // Status będzie ustawiany w serwisie (np. PENDING, COMPLETED)

    // fromAccount i toAccount będą pobierane z repozytorium w serwisie na podstawie fromAccountId/toAccountId z requestu
    @Mapping(target = "fromAccount", ignore = true)
    @Mapping(target = "toAccount", ignore = true)

    // Pola mapowane bezpośrednio z TransactionRequest na encję
    @Mapping(source = "transactionType", target = "transactionType") // Użyj wartości z TransactionRequest
    @Mapping(source = "amount", target = "amount")
    @Mapping(source = "currency", target = "currency")
    @Mapping(source = "description", target = "description")
    Transaction toEntity(TransactionRequest transactionRequest);
}