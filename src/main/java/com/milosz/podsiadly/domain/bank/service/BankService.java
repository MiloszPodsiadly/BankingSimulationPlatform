package com.milosz.podsiadly.domain.bank.service;

import com.milosz.podsiadly.domain.bank.model.Bank;
import com.milosz.podsiadly.domain.bank.repository.BankRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BankService {

    private final BankRepository bankRepository;

    public BankService(BankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    @Transactional
    public Bank createBank(Bank bank) {
        // Można dodać walidację biznesową, np. czy BIC jest unikalny
        if (bankRepository.findByBic(bank.getBic()).isPresent()) {
            throw new IllegalArgumentException("Bank with this BIC exist.");
        }
        return bankRepository.save(bank);
    }

    @Transactional(readOnly = true)
    public Optional<Bank> getBankById(Long id) {
        return bankRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Bank> getBankByName(String name) {
        return bankRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public List<Bank> getAllBanks() {
        return bankRepository.findAll();
    }

    @Transactional
    public Bank updateBank(Long id, Bank updatedBank) {
        Bank existingBank = bankRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bank with ID " + id + " not found."));

        existingBank.setName(updatedBank.getName());
        existingBank.setBic(updatedBank.getBic());
        existingBank.setAddress(updatedBank.getAddress());
        existingBank.setContactEmail(updatedBank.getContactEmail());

        return bankRepository.save(existingBank);
    }

    @Transactional
    public void deleteBank(Long id) {
        if (!bankRepository.existsById(id)) {
            throw new EntityNotFoundException("Bank with ID " + id + " not found.");
        }
        // Należy rozważyć obsługę kont bankowych powiązanych z tym bankiem przed usunięciem
        bankRepository.deleteById(id);
    }
}