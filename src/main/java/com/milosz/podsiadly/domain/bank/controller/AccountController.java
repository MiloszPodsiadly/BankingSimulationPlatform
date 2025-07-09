package com.milosz.podsiadly.domain.bank.controller;

import com.milosz.podsiadly.domain.bank.dto.AccountDto;
import com.milosz.podsiadly.domain.bank.mapper.AccountMapper;
import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.service.AccountService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    public AccountController(AccountService accountService, AccountMapper accountMapper) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody AccountDto accountDto, @RequestParam Long bankId) {
        try {
            // W przypadku tworzenia, id encji jest generowane automatycznie, więc ignorujemy je w mapowaniu do encji.
            // Bank jest ustawiany w serwisie na podstawie bankId.
            BankAccount accountToCreate = accountMapper.toEntity(accountDto);
            BankAccount createdAccount = accountService.createAccount(accountToCreate, bankId);
            return new ResponseEntity<>(accountMapper.toDto(createdAccount), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // Zwróć błąd wraz z informacją
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Bank nie znaleziony
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        return accountService.getAccountById(id)
                .map(accountMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountDto>> getAccountsByUserId(@PathVariable Long userId) {
        List<AccountDto> accounts = accountService.getAccountsByUserId(userId).stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(accounts);
    }

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        List<AccountDto> accounts = accountService.getAllBankAccounts().stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> updateAccount(@PathVariable Long id, @Valid @RequestBody AccountDto accountDto) {
        try {
            // Zakładamy, że AccountDto służy do aktualizacji tylko pewnych pól (np. balance, currency, status)
            // accountNumber, userId, bankId są niezmienne po utworzeniu
            BankAccount accountToUpdate = accountMapper.toEntity(accountDto); // MapStruct może zmapować tylko te pola, które są w DTO
            BankAccount updatedAccount = accountService.updateAccount(id, accountToUpdate);
            return ResponseEntity.ok(accountMapper.toDto(updatedAccount));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        try {
            accountService.deleteAccount(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
