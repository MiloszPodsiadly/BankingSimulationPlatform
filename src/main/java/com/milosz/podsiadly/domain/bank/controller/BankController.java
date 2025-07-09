package com.milosz.podsiadly.domain.bank.controller;

import com.milosz.podsiadly.domain.bank.dto.BankDto;
import com.milosz.podsiadly.domain.bank.mapper.BankMapper;
import com.milosz.podsiadly.domain.bank.model.Bank;
import com.milosz.podsiadly.domain.bank.service.BankService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/banks")
public class BankController {

    private final BankService bankService;
    private final BankMapper bankMapper;

    public BankController(BankService bankService, BankMapper bankMapper) {
        this.bankService = bankService;
        this.bankMapper = bankMapper;
    }

    @PostMapping
    public ResponseEntity<BankDto> createBank(@Valid @RequestBody BankDto bankDto) {
        try {
            Bank bank = bankMapper.toEntity(bankDto);
            Bank createdBank = bankService.createBank(bank);
            return new ResponseEntity<>(bankMapper.toDto(createdBank), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankDto> getBankById(@PathVariable Long id) {
        return bankService.getBankById(id)
                .map(bankMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<BankDto>> getAllBanks() {
        List<BankDto> banks = bankService.getAllBanks().stream()
                .map(bankMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(banks);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BankDto> updateBank(@PathVariable Long id, @Valid @RequestBody BankDto bankDto) {
        try {
            Bank updatedBank = bankService.updateBank(id, bankMapper.toEntity(bankDto));
            return ResponseEntity.ok(bankMapper.toDto(updatedBank));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBank(@PathVariable Long id) {
        try {
            bankService.deleteBank(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
