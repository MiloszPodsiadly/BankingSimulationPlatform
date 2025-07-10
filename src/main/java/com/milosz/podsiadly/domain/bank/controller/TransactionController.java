package com.milosz.podsiadly.domain.bank.controller;

import com.milosz.podsiadly.common.exception.ResourceNotFoundException;
import com.milosz.podsiadly.domain.bank.dto.TransactionDto;
import com.milosz.podsiadly.domain.bank.dto.TransactionRequest;
import com.milosz.podsiadly.domain.bank.mapper.TransactionMapper;
import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.bank.service.AccountService;
import com.milosz.podsiadly.domain.bank.service.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.kafka.common.requests.DeleteAclsResponse.log;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService; // Potrzebne do pobrania konta po numerze
    private final TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService, AccountService accountService, TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.transactionMapper = transactionMapper;
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> createTransfer(@Valid @RequestBody TransactionRequest request) {
        String username;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            username = authentication.getName();
        } else {
            log.warn("Unauthorized attempt to create transfer. No authenticated user found.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Get Source Account - NOW PASSING USERNAME
            BankAccount sourceAccount = accountService.getAccountById(request.sourceAccountId(), username);
            // .orElseThrow is no longer needed here as getAccountById throws ResourceNotFoundException directly

            // Get Target Account - Assuming getAccountByAccountNumber does NOT require a username
            // If getAccountByAccountNumber returns Optional, you still need .orElseThrow()
            Optional<BankAccount> targetAccountOptional = accountService.getAccountByAccountNumber(request.targetAccountNumber());
            BankAccount targetAccount = targetAccountOptional
                    .orElseThrow(() -> new ResourceNotFoundException("Konto docelowe o numerze " + request.targetAccountNumber() + " nie znaleziono."));


            // Ustaw typ transakcji w encji, ponieważ TransactionRequest jest ogólny
            Transaction transaction = transactionMapper.toEntity(request);
            transaction.setSourceAccount(sourceAccount);
            transaction.setTargetAccount(targetAccount);
            transaction.setType(Transaction.TransactionType.TRANSFER); // Upewniamy się, że typ jest ustawiony
            // You might also want to set currency and description if not set by mapper or request
            transaction.setCurrency(request.currency()); // Ensure currency is set
            transaction.setDescription("Transfer from " + sourceAccount.getAccountNumber() + " to " + targetAccount.getAccountNumber());


            Transaction processedTransaction = transactionService.processTransaction(transaction);
            return new ResponseEntity<>(transactionMapper.toDto(processedTransaction), HttpStatus.CREATED);

        } catch (ResourceNotFoundException e) { // Catch ResourceNotFoundException directly from service
            log.warn("Transfer failed due to missing account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Transfer failed due to invalid arguments or state: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) { // Catch any other unexpected exceptions
            log.error("An unexpected error occurred during transfer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Możesz dodać endpointy dla innych typów transakcji, np. deposit, withdrawal
    @PostMapping("/deposit")
    public ResponseEntity<TransactionDto> createDeposit(@Valid @RequestBody TransactionRequest request) {
        try {
            BankAccount targetAccount = accountService.getAccountByAccountNumber(request.targetAccountNumber())
                    .orElseThrow(() -> new EntityNotFoundException("Konto docelowe o numerze " + request.targetAccountNumber() + " nie znaleziono."));

            Transaction transaction = transactionMapper.toEntity(request);
            transaction.setTargetAccount(targetAccount);
            transaction.setSourceAccount(null); // Dla depozytu nie ma konta źródłowego w naszym systemie
            transaction.setType(Transaction.TransactionType.DEPOSIT);

            Transaction processedTransaction = transactionService.processTransaction(transaction);
            return new ResponseEntity<>(transactionMapper.toDto(processedTransaction), HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/withdrawal")
    public ResponseEntity<TransactionDto> createWithdrawal(@Valid @RequestBody TransactionRequest request) {
        String username;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            username = authentication.getName();
        } else {
            log.warn("Unauthorized attempt to create withdrawal. No authenticated user found.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Correctly call accountService.getAccountById with both accountId and username
            // Since getAccountById throws ResourceNotFoundException directly, .orElseThrow is not needed here.
            BankAccount sourceAccount = accountService.getAccountById(request.sourceAccountId(), username);

            Transaction transaction = transactionMapper.toEntity(request);
            transaction.setSourceAccount(sourceAccount);
            transaction.setTargetAccount(null); // For withdrawal, no target account in our system
            transaction.setType(Transaction.TransactionType.WITHDRAWAL);
            transaction.setCurrency(request.currency()); // Ensure currency is set
            transaction.setDescription("Withdrawal from " + sourceAccount.getAccountNumber()); // Default description


            Transaction processedTransaction = transactionService.processTransaction(transaction);
            return new ResponseEntity<>(transactionMapper.toDto(processedTransaction), HttpStatus.CREATED);

        } catch (ResourceNotFoundException e) { // Catch ResourceNotFoundException directly
            log.warn("Withdrawal failed due to missing account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Withdrawal failed due to invalid arguments or state: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) { // Catch any other unexpected exceptions
            log.error("An unexpected error occurred during withdrawal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionById(id)
                .map(transactionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/ref/{transactionRef}")
    public ResponseEntity<TransactionDto> getTransactionByRef(@PathVariable String transactionRef) {
        return transactionService.getTransactionByRef(transactionRef)
                .map(transactionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionDto>> getTransactionsByAccountId(@PathVariable Long accountId) {
        List<TransactionDto> transactions = transactionService.getTransactionsByAccountId(accountId).stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/account/{accountId}/history")
    public ResponseEntity<List<TransactionDto>> getTransactionsByAccountIdAndDateRange(
            @PathVariable Long accountId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<TransactionDto> transactions = transactionService.getTransactionsByAccountIdAndDateRange(accountId, startDate, endDate).stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(transactions);
    }

    @GetMapping
    public ResponseEntity<List<TransactionDto>> getAllTransactions() {
        List<TransactionDto> transactions = transactionService.getAllTransactions().stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(transactions);
    }
}
