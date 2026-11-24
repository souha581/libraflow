package com.libraflow.service;

import com.libraflow.model.Book;
import com.libraflow.model.LoanStatus;
import com.libraflow.repository.BookRepository;
import com.libraflow.repository.LoanRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, loanRepository, new SimpleMeterRegistry());
    }

    @Test
    void createBookSucceedsWhenIsbnIsUnique() {
        when(bookRepository.existsByIsbn("978-1")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book created = bookService.createBook("DevOps", "Kim", "978-1", 3);

        assertEquals("DevOps", created.getTitle());
        assertEquals(3, created.getTotalCopies());
        assertEquals(3, created.getAvailableCopies());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void createBookThrowsWhenIsbnAlreadyExists() {
        when(bookRepository.existsByIsbn("978-1")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> bookService.createBook("DevOps", "Kim", "978-1", 2));
    }

    @Test
    void searchBooksReturnsAllWhenQueryIsBlank() {
        Book b = new Book("Clean Code", "Martin", "978-2", 2);
        when(bookRepository.findAll()).thenReturn(List.of(b));

        List<Book> result = bookService.searchBooks("   ");

        assertEquals(1, result.size());
        verify(bookRepository).findAll();
    }

    @Test
    void deleteBookThrowsWhenActiveLoanExists() {
        Book b = new Book("Clean Code", "Martin", "978-2", 2);
        b.setId(10L);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(b));
        when(loanRepository.existsByBookIdAndStatus(10L, LoanStatus.ACTIVE)).thenReturn(true);

        assertThrows(BusinessException.class, () -> bookService.deleteBook(10L));
    }
}
