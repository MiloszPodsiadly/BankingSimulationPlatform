package com.milosz.podsiadly.domain.bank.controller;

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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
        try {
            BankAccount sourceAccount = accountService.getAccountById(request.sourceAccountId())
                    .orElseThrow(() -> new EntityNotFoundException("Konto źródłowe o ID " + request.sourceAccountId() + " nie znaleziono."));
            BankAccount targetAccount = accountService.getAccountByAccountNumber(request.targetAccountNumber())
                    .orElseThrow(() -> new EntityNotFoundException("Konto docelowe o numerze " + request.targetAccountNumber() + " nie znaleziono."));

            // Ustaw typ transakcji w encji, ponieważ TransactionRequest jest ogólny
            Transaction transaction = transactionMapper.toEntity(request);
            transaction.setSourceAccount(sourceAccount);
            transaction.setTargetAccount(targetAccount);
            transaction.setType(Transaction.TransactionType.TRANSFER); // Upewniamy się, że typ jest ustawiony

            Transaction processedTransaction = transactionService.processTransaction(transaction);
            return new ResponseEntity<>(transactionMapper.toDto(processedTransaction), HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
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
        try {
            BankAccount sourceAccount = accountService.getAccountById(request.sourceAccountId())
                    .orElseThrow(() -> new EntityNotFoundException("Konto źródłowe o ID " + request.sourceAccountId() + " nie znaleziono."));

            Transaction transaction = transactionMapper.toEntity(request);
            transaction.setSourceAccount(sourceAccount);
            transaction.setTargetAccount(null); // Dla wypłaty nie ma konta docelowego w naszym systemie
            transaction.setType(Transaction.TransactionType.WITHDRAWAL);

            Transaction processedTransaction = transactionService.processTransaction(transaction);
            return new ResponseEntity<>(transactionMapper.toDto(processedTransaction), HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
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
