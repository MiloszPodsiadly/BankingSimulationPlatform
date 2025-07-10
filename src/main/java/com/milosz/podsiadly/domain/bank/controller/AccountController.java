package com.milosz.podsiadly.domain.bank.controller;

import com.milosz.podsiadly.common.exception.ResourceNotFoundException;
import com.milosz.podsiadly.domain.bank.dto.AccountDto;
import com.milosz.podsiadly.domain.bank.mapper.AccountMapper;
import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.service.AccountService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.kafka.common.requests.DeleteAclsResponse.log;

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
    public ResponseEntity<AccountDto> createAccount(
            @Valid @RequestBody AccountDto accountDto, // Use your existing AccountDto for the request body
            @RequestParam Long bankId) { // bankId is passed as a query parameter

        // Retrieve the username from the security context
        String username;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            username = authentication.getName();
        } else {
            // Fallback for unauthenticated requests or simulation.
            // In a real API, you might throw an Unauthorized exception here.
            log.warn("Account creation request received without authenticated user. Using 'anonymous' username.");
            username = "anonymous";
        }

        try {
            // IMPORTANT: Your AccountDto does not have an 'accountType' field.
            // The createBankAccount service method requires 'accountType'.
            // For this specific change, I'm using a placeholder.
            // Ideally, 'accountType' should be added to your AccountDto if it's user-provided.
            String accountTypeForService = "CHECKING"; // <-- Placeholder/Default value for accountType

            // The 'bankId' from @RequestParam will take precedence over bankId in the DTO body
            // if both are present, as @RequestParam is explicitly parsed.
            // We'll use the @RequestParam `bankId` as it's cleaner for REST.

            BankAccount createdAccount = accountService.createBankAccount(
                    accountDto.userId(),      // Get userId from the DTO record
                    bankId,                   // Get bankId from the @RequestParam
                    accountTypeForService,    // Use the placeholder/default account type
                    accountDto.currency(),    // Get currency from the DTO record
                    username                  // Get username from the security context
            );

            // Map the created BankAccount entity back to the AccountDto for the response
            return new ResponseEntity<>(accountMapper.toDto(createdAccount), HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request for account creation: {}", e.getMessage());
            // You might want to return a more structured error response here
            return ResponseEntity.badRequest().body(null);
        } catch (ResourceNotFoundException e) { // Catch ResourceNotFoundException specifically
            log.error("Related resource (User or Bank) not found during account creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            log.error("An unexpected error occurred during account creation: {}", e.getMessage(), e);
            // Catch any other unexpected exceptions
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable("id") Long accountId) { // Use accountId for clarity

        String username;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            username = authentication.getName();
        } else {
            // If no authenticated user, return UNAUTHORIZED or FORBIDDEN.
            // In a real application, you likely wouldn't allow unauthenticated access to specific account details.
            log.warn("Unauthorized attempt to access account by ID: {}. No authenticated user found.", accountId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // Or HttpStatus.FORBIDDEN
        }

        try {
            // Call the service method with both required arguments: accountId and username
            BankAccount account = accountService.getAccountById(accountId, username);
            // Since accountService.getAccountById(accountId, username) returns BankAccount directly
            // (not an Optional), we no longer need the .map().orElseGet() chain here.
            return ResponseEntity.ok(accountMapper.toDto(account));

        } catch (ResourceNotFoundException e) {
            log.warn("Account not found with ID: {} for user {}. Error: {}", accountId, username, e.getMessage());
            return ResponseEntity.notFound().build(); // HTTP 404
        } catch (Exception e) {
            log.error("An unexpected error occurred while retrieving account ID {} for user {}: {}", accountId, username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // HTTP 500
        }
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
        // Retrieve the username from the security context
        String username;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            username = authentication.getName();
        } else {
            log.warn("Unauthorized attempt to update account ID: {}. No authenticated user found.", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // Return 401 if not authenticated
        }

        try {
            // Correctly call accountService.updateAccount with:
            // 1. The ID from @PathVariable
            // 2. The AccountDto directly from @RequestBody (not mapped to entity here)
            // 3. The username from the security context
            BankAccount updatedAccount = accountService.updateAccount(id, accountDto, username);

            // Map the updated BankAccount entity back to AccountDto for the response
            return ResponseEntity.ok(accountMapper.toDto(updatedAccount));

        } catch (ResourceNotFoundException e) { // Catch ResourceNotFoundException from service
            log.warn("Account not found with ID: {} for user {} during update: {}", id, username, e.getMessage());
            return ResponseEntity.notFound().build(); // HTTP 404 Not Found
        } catch (IllegalArgumentException e) { // Catch IllegalArgumentException from service
            log.error("Invalid arguments for updating account ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(null); // HTTP 400 Bad Request
        } catch (Exception e) { // Catch any other unexpected exceptions
            log.error("An unexpected error occurred while updating account ID {} for user {}: {}", id, username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // HTTP 500 Internal Server Error
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        String username;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            username = authentication.getName();
        } else {
            // If no authenticated user, return UNAUTHORIZED or FORBIDDEN.
            // This is a protected endpoint, so unauthenticated access should be denied.
            log.warn("Unauthorized attempt to delete account ID: {}. No authenticated user found.", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Call the service method with both required arguments: the ID and the username
            accountService.deleteAccount(id, username);
            return ResponseEntity.noContent().build(); // HTTP 204 No Content for successful deletion

        } catch (ResourceNotFoundException e) { // Catch ResourceNotFoundException from service
            log.warn("Account not found with ID: {} for user {} during deletion: {}", id, username, e.getMessage());
            return ResponseEntity.notFound().build(); // HTTP 404 Not Found
        } catch (Exception e) { // Catch any other unexpected exceptions
            log.error("An unexpected error occurred while deleting account ID {} for user {}: {}", id, username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // HTTP 500 Internal Server Error
        }
    }
}
