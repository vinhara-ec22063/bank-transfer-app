package com.example.bankapp.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("---> DataLoader is running! Current customer count: " + customerRepository.count());

        // Insert sample data only if no customers exist yet (Single check)
        if (customerRepository.count() == 0) {
            System.out.println("---> Inserting sample customers...");
            
            Customer c1 = new Customer();
            c1.setAccountNumber("100001");
            c1.setFullName("Kamal Perera");
            c1.setEmail("kamal@example.com");
            c1.setBalance(new BigDecimal("50000.00"));
            customerRepository.save(c1);

            Customer c2 = new Customer();
            c2.setAccountNumber("100002");
            c2.setFullName("Nimali Silva");
            c2.setEmail("nimali@example.com");
            c2.setBalance(new BigDecimal("25000.00"));
            customerRepository.save(c2);

            Customer c3 = new Customer();
            c3.setAccountNumber("100003");
            c3.setFullName("Saman Kumara");
            c3.setEmail("saman@example.com");
            c3.setBalance(new BigDecimal("10000.00"));
            customerRepository.save(c3);

            System.out.println("---> Sample customers inserted successfully!");
        } else {
            System.out.println("---> Customers already exist in database.");
        }
    }
}