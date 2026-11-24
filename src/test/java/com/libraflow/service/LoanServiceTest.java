package com.libraflow.service;

import com.libraflow.model.AppUser;
import com.libraflow.model.Book;
import com.libraflow.model.Loan;
import com.libraflow.model.LoanStatus;
import com.libraflow.model.Role;
import com.libraflow.repository.AppUserRepository;
import com.libraflow.repository.BookRepository;
import com.libraflow.repository.LoanRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AppUserRepository userRepository;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(
                loanRepository, bookRepository, userRepository, new SimpleMeterRegistry()
        );
    }

    @Test
    void borrowBookDecrementsAvailableCopies() {
        Book book = new Book("Clean Code", "Martin", "978-2", 2);
        book.setId(1L);
        AppUser user = new AppUser("etudiant", "hash", "Etudiant", Role.MEMBER);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findByUsername("etudiant")).thenReturn(Optional.of(user));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan loan = loanService.borrowBook(1L, "etudiant");

        assertEquals(LoanStatus.ACTIVE, loan.getStatus());
        assertEquals(1, book.getAvailableCopies());
    }

    @Test
    void borrowBookThrowsWhenOutOfStock() {
        Book book = new Book("Clean Code", "Martin", "978-2", 1);
        book.setId(1L);
        book.setAvailableCopies(0);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThrows(BusinessException.class, () -> loanService.borrowBook(1L, "etudiant"));
    }

    @Test
    void returnBookRestoresStockAndMarksReturned() {
        Book book = new Book("Clean Code", "Martin", "978-2", 2);
        book.setAvailableCopies(1);
        AppUser user = new AppUser("etudiant", "hash", "Etudiant", Role.MEMBER);
        Loan loan = new Loan(book, user);
        loan.setId(5L);

        when(loanRepository.findById(5L)).thenReturn(Optional.of(loan));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan returned = loanService.returnBook(5L, "etudiant", false);

        assertEquals(LoanStatus.RETURNED, returned.getStatus());
        assertNotNull(returned.getReturnedAt());
        assertEquals(2, book.getAvailableCopies());
    }

    @Test
    void returnBookThrowsWhenUnauthorizedMember() {
        Book book = new Book("Clean Code", "Martin", "978-2", 2);
        AppUser owner = new AppUser("alice", "hash", "Alice", Role.MEMBER);
        Loan loan = new Loan(book, owner);
        loan.setId(5L);

        when(loanRepository.findById(5L)).thenReturn(Optional.of(loan));

        assertThrows(BusinessException.class,
                () -> loanService.returnBook(5L, "bob", false));
    }
}
