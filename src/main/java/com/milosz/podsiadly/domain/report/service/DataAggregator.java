package com.milosz.podsiadly.domain.report.service;

import com.milosz.podsiadly.core.event.AccountCreatedEvent;
import com.milosz.podsiadly.core.event.TransactionCompletedEvent;
import com.milosz.podsiadly.core.event.TransactionFailedEvent;
import com.milosz.podsiadly.core.event.UserRegisteredEvent;
import com.milosz.podsiadly.domain.bank.model.BankAccount; // DODANE: Import BankAccount
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.bank.repository.BankAccountRepository; // DODANE: Import BankAccountRepository
import com.milosz.podsiadly.domain.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for aggregating and preparing data for various reports.
 * It consumes events from Kafka and stores/processes them for reporting purposes.
 * It also provides methods to retrieve aggregated data for report generation.
 */
@Service
@RequiredArgsConstructor
public class DataAggregator {

    private static final Logger log = LoggerFactory.getLogger(DataAggregator.class);

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository; // DODANE: Wstrzykujemy BankAccountRepository

    // Metody do przetwarzania zdarzeń (jak wcześniej)
    public void processAccountCreationEvent(AccountCreatedEvent event) {
        log.info("DataAggregator: Processing AccountCreatedEvent for account ID: {}", event.getAccountId());
        // Tutaj logika agregacji danych dla raportów związanych z tworzeniem kont
        // np. zapis do bazy danych raportów, aktualizacja statystyk
    }

    public void processTransactionCompletedEvent(TransactionCompletedEvent event) {
        log.info("DataAggregator: Processing TransactionCompletedEvent for transaction ID: {}", event.getTransactionId());
        // Tutaj logika agregacji danych dla raportów związanych z pomyślnymi transakcjami
    }

    public void processTransactionFailedEvent(TransactionFailedEvent event) {
        log.warn("DataAggregator: Processing TransactionFailedEvent for transaction ID: {}. Reason: {}", event.getTransactionId(), event.getReason());
        // Tutaj logika agregacji danych dla raportów związanych z nieudanymi transakcjami
    }

    public void processUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("DataAggregator: Processing UserRegisteredEvent for user ID: {}", event.getUserId());
        // Tutaj logika agregacji danych dla raportów związanych z rejestracją użytkowników
    }

    /**
     * Retrieves a list of transactions within a specified date range.
     * This method is used by ReportGenerator to fetch data for transaction history reports.
     *
     * @param startDate The start date and time of the period.
     * @param endDate The end date and time of the period.
     * @return A list of Transaction entities.
     */
    public List<Transaction> getTransactionsForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("DataAggregator: Fetching transactions for period from {} to {}", startDate, endDate);
        // Pobieramy wszystkie transakcje w danym zakresie dat.
        // W TransactionRepository musisz mieć metodę findByTransactionDateBetween.
        return transactionRepository.findByTransactionDateBetween(startDate, endDate);
    }

    /**
     * Aggregates data for the Profit and Loss Statement for a given period.
     * This is a simplified aggregation. In a real bank, revenue and expenses
     * would be derived from specific transaction types (e.g., interest income, fees, salaries).
     *
     * @param startDate Start of the reporting period.
     * @param endDate End of the reporting period.
     * @return A map containing aggregated P&L data (e.g., totalRevenue, totalExpenses, netProfitLoss).
     */
    public Map<String, BigDecimal> aggregateProfitAndLossData(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("DataAggregator: Aggregating P&L data for period: {} to {}", startDate, endDate);

        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(startDate, endDate);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            // Uproszczona logika:
            // Zakładamy, że DEPOSIT i INTEREST_PAYOUT to przychody (dla banku, w uproszczeniu)
            // Zakładamy, że WITHDRAWAL i FEE to koszty (dla banku, w uproszczeniu)
            // TRANSFER nie wpływa na P&L banku, tylko na salda klientów
            switch (transaction.getType()) {
                case DEPOSIT:
                case INTEREST_PAYOUT:
                    // Jeśli bank zarabia na wpłatach lub wypłaca odsetki (co jest kosztem, ale tu upraszczamy)
                    // To jest bardzo uproszczone. W realu, odsetki płacone klientom to koszt, odsetki z kredytów to przychód.
                    if (transaction.getTargetAccount() != null) { // Wpłaty na konta klientów
                        totalRevenue = totalRevenue.add(transaction.getAmount());
                    }
                    break;
                case WITHDRAWAL:
                case FEE:
                    // Jeśli bank pobiera opłaty (przychód) lub klient wypłaca (zmniejsza aktywa banku, ale nie jest kosztem)
                    // To jest również bardzo uproszczone. Opłaty to przychód banku. Wypłaty to zmniejszenie pasywów.
                    if (transaction.getSourceAccount() != null) {
                        totalExpenses = totalExpenses.add(transaction.getAmount());
                    }
                    break;
                // LOAN_REPAYMENT - wpływa na bilans, niekoniecznie na P&L bezpośrednio (chyba że odsetki)
                // TRANSFER - nie wpływa na P&L banku jako całości
            }
        }

        BigDecimal netProfitLoss = totalRevenue.subtract(totalExpenses);

        Map<String, BigDecimal> pnlData = new HashMap<>();
        pnlData.put("totalRevenue", totalRevenue);
        pnlData.put("totalExpenses", totalExpenses);
        pnlData.put("netProfitLoss", netProfitLoss);

        return pnlData;
    }

    /**
     * Aggregates data for the Balance Sheet as of the current moment.
     * This is a highly simplified aggregation. In a real bank, assets include loans, investments,
     * cash reserves, while liabilities include customer deposits, borrowings, etc.
     *
     * @return A map containing aggregated Balance Sheet data (e.g., totalAssets, totalLiabilities, totalEquity).
     */
    public Map<String, BigDecimal> aggregateBalanceSheetData() {
        log.info("DataAggregator: Aggregating Balance Sheet data as of current date.");

        List<BankAccount> allAccounts = bankAccountRepository.findAll();

        // Uproszczona logika:
        // Całkowite saldo wszystkich kont klientów (depozyty) traktujemy jako zobowiązania banku.
        BigDecimal totalLiabilities = allAccounts.stream()
                .map(BankAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Dla uproszczenia, zakładamy, że aktywa banku są równe zobowiązaniom z tytułu depozytów klientów.
        // W rzeczywistości aktywa banku to m.in. udzielone kredyty, inwestycje, rezerwy gotówkowe.
        BigDecimal totalAssets = totalLiabilities; // Bardzo duże uproszczenie!

        // Kapitał własny (equity) = Aktywa - Pasywa. W tym uproszczonym modelu może być zero.
        BigDecimal totalEquity = totalAssets.subtract(totalLiabilities); // Będzie zero w tym uproszczeniu

        Map<String, BigDecimal> balanceSheetData = new HashMap<>();
        balanceSheetData.put("totalAssets", totalAssets);
        balanceSheetData.put("totalLiabilities", totalLiabilities);
        balanceSheetData.put("totalEquity", totalEquity); // W tym uproszczeniu będzie 0

        return balanceSheetData;
    }
}
