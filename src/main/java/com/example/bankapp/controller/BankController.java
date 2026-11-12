package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class BankController {
    private final BankService bankService;
    private final TransactionAuditService auditService;

    // Dashboard: statistics, form, customers table, latest transactions
    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("customers", bankService.getAllCustomers(keyword));
        model.addAttribute("transactions", bankService.getAllTransactions());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount", bankService.getCountByStatus(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankService.getCountByStatus(TransactionStatus.FAILED));
        
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
        return "bank";
    }

    // Add a customer
    @PostMapping("/customers")
    public String addCustomer(@Valid @ModelAttribute("customer") Customer customer,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customer", result);
            redirectAttributes.addFlashAttribute("customer", customer);
            return "redirect:/";
        }
        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("successMessage", "Customer added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/";
    }

    // Transfer money: calls service inside try, catch block runs after rollback
    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.transferRequest", result);
            redirectAttributes.addFlashAttribute("transferRequest", request);
            return "redirect:/";
        }

        try {
            bankService.transfer(request);
            redirectAttributes.addFlashAttribute("successMessage", "Transfer completed successfully!");
        } catch (Exception e) {
            // After the transfer transaction rolls back, record the FAILED entry in a new transaction
            auditService.recordFailure(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    e.getMessage()
            );
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/";
    }

    // Transactions history of one account
    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(@PathVariable String accountNumber, Model model) {
        model.addAttribute("accountNumber", accountNumber);
        model.addAttribute("history", bankService.getAccountHistory(accountNumber));
        return "history";
    }

}
