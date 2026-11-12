package com.example.bankapp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BankService { 
    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    public List<Customer> getAllCustomers(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return customerRepository.searchByName(keyword);
        }
        return customerRepository.findAllOrdered();
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
            throw new BankException("Account number already exists: " + customer.getAccountNumber());
        }
        if (customerRepository.countEmails(customer.getEmail()) > 0) {
            throw new BankException("Email already exists: " + customer.getEmail());
        }
        return customerRepository.save(customer);
    }

    @Transactional(rollbackFor = Exception.class)
    public void transfer(TransferRequest request) {
        String fromAcc = request.getFromAccount();
        String toAcc = request.getToAccount();
        BigDecimal amount = request.getAmount();

        // Step 1: Validate accounts are different and both exist
        if (fromAcc.equals(toAcc)) {
            throw new BankException("Source and destination accounts must be different");
        }

        Customer sender = customerRepository.getByAccountNumber(fromAcc)
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found: " + fromAcc));
        Customer receiver = customerRepository.getByAccountNumber(toAcc)
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found: " + toAcc));

        // Step 2: debit(sender, amount): UPDATE sender's balance atomically
        int updatedRows = customerRepository.debit(fromAcc, amount);
        if (updatedRows == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + fromAcc);
        }

        // Step 3: (Experiment) throw a simulated failure if checked
        if (request.isSimulateFailure()) {
            throw new BankException("Simulated system failure after the withdrawal");
        }

        // Step 4: credit(receiver, amount): UPDATE receiver's balance
        customerRepository.credit(toAcc, amount);

        // Step 5: Save a SUCCESS record in bank transactions
        BankTransaction tx = new BankTransaction();
        tx.setFromAccount(fromAcc);
        tx.setToAccount(toAcc);
        tx.setAmount(amount);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setDescription("Transfer completed");
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<BankTransaction> getAllTransactions() {
        return transactionRepository.findAllLatest();
    }

    public List<BankTransaction> getAccountHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public long getCountByStatus(TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }
}