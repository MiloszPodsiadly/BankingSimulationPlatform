package com.milosz.podsiadly.domain.bank.controller;

import com.milosz.podsiadly.domain.bank.dto.LoanApplicationDto;
import com.milosz.podsiadly.domain.bank.dto.LoanDto;
import com.milosz.podsiadly.domain.bank.mapper.LoanMapper;
import com.milosz.podsiadly.domain.bank.model.Loan;
import com.milosz.podsiadly.domain.bank.service.LoanService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;
    private final LoanMapper loanMapper;

    public LoanController(LoanService loanService, LoanMapper loanMapper) {
        this.loanService = loanService;
        this.loanMapper = loanMapper;
    }

    @PostMapping("/apply")
    public ResponseEntity<LoanDto> applyForLoan(@Valid @RequestBody LoanApplicationDto loanApplicationDto) {
        try {
            // LoanApplicationDto jest mapowane na Loan, ale pola takie jak loanNumber, status, dates
            // są ustawiane w serwisie.accountId jest używany do pobrania BankAccount.
            Loan newLoan = loanMapper.toEntity(loanApplicationDto);
            Loan createdLoan = loanService.createLoan(newLoan, loanApplicationDto.accountId());
            return new ResponseEntity<>(loanMapper.toDto(createdLoan), HttpStatus.CREATED);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanDto> getLoanById(@PathVariable Long id) {
        return loanService.getLoanById(id)
                .map(loanMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<LoanDto>> getLoansByAccountId(@PathVariable Long accountId) {
        List<LoanDto> loans = loanService.getLoansByAccountId(accountId).stream()
                .map(loanMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(loans);
    }

    @GetMapping
    public ResponseEntity<List<LoanDto>> getAllLoans() {
        List<LoanDto> loans = loanService.getAllLoans().stream()
                .map(loanMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(loans);
    }

    @PostMapping("/{loanId}/repay")
    public ResponseEntity<LoanDto> repayLoan(@PathVariable Long loanId, @RequestParam BigDecimal amount) {
        try {
            Loan updatedLoan = loanService.makeLoanRepayment(loanId, amount);
            return ResponseEntity.ok(loanMapper.toDto(updatedLoan));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}/status/{newStatus}")
    public ResponseEntity<LoanDto> updateLoanStatus(@PathVariable Long id, @PathVariable Loan.LoanStatus newStatus) {
        try {
            Loan updatedLoan = loanService.updateLoanStatus(id, newStatus);
            return ResponseEntity.ok(loanMapper.toDto(updatedLoan));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        try {
            loanService.deleteLoan(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
