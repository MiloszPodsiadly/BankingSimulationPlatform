package com.milosz.podsiadly.domain.bank.controller;

import com.milosz.podsiadly.domain.bank.dto.DepositDto;
import com.milosz.podsiadly.domain.bank.mapper.DepositMapper;
import com.milosz.podsiadly.domain.bank.model.Deposit;
import com.milosz.podsiadly.domain.bank.service.DepositService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deposits")
public class DepositController {

    private final DepositService depositService;
    private final DepositMapper depositMapper;

    public DepositController(DepositService depositService, DepositMapper depositMapper) {
        this.depositService = depositService;
        this.depositMapper = depositMapper;
    }

    @PostMapping
    public ResponseEntity<DepositDto> createDeposit(@Valid @RequestBody DepositDto depositDto, @RequestParam Long accountId) {
        try {
            // Mapujemy DepositDto na Deposit, ale account i inne pola są ustawiane w serwisie
            Deposit newDeposit = depositMapper.toEntity(depositDto);
            Deposit createdDeposit = depositService.createDeposit(newDeposit, accountId);
            return new ResponseEntity<>(depositMapper.toDto(createdDeposit), HttpStatus.CREATED);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepositDto> getDepositById(@PathVariable Long id) {
        return depositService.getDepositById(id)
                .map(depositMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<DepositDto>> getDepositsByAccountId(@PathVariable Long accountId) {
        List<DepositDto> deposits = depositService.getDepositsByAccountId(accountId).stream()
                .map(depositMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(deposits);
    }

    @GetMapping
    public ResponseEntity<List<DepositDto>> getAllDeposits() {
        List<DepositDto> deposits = depositService.getAllDeposits().stream()
                .map(depositMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(deposits);
    }

    @PutMapping("/{id}/status/{newStatus}")
    public ResponseEntity<DepositDto> updateDepositStatus(@PathVariable Long id, @PathVariable Deposit.DepositStatus newStatus) {
        try {
            Deposit updatedDeposit = depositService.updateDepositStatus(id, newStatus);
            return ResponseEntity.ok(depositMapper.toDto(updatedDeposit));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/mature")
    public ResponseEntity<Void> matureDeposit(@PathVariable Long id) {
        try {
            depositService.matureDeposit(id);
            return ResponseEntity.noContent().build(); // Brak zawartości, bo środki są transferowane
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeposit(@PathVariable Long id) {
        try {
            depositService.deleteDeposit(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
