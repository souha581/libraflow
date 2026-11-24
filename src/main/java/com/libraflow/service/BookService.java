package com.libraflow.service;

import com.libraflow.model.Book;
import com.libraflow.model.LoanStatus;
import com.libraflow.repository.BookRepository;
import com.libraflow.repository.LoanRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing book catalog operations and stock validation.
 */
@Service
public class BookService {

    private static final Logger LOG = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final Counter booksCreatedCounter;

    public BookService(BookRepository bookRepository,
                       LoanRepository loanRepository,
                       MeterRegistry meterRegistry) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.booksCreatedCounter = Counter.builder("libraflow.books.created")
                .description("Total number of books added to catalog")
                .register(meterRegistry);
    }

    public List<Book> searchBooks(String query) {
        if (query == null || query.isBlank()) {
            return bookRepository.findAll();
        }
        String trimmed = query.trim();
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(trimmed, trimmed);
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Livre introuvable (id=" + id + ")"));
    }

    @Transactional
    public Book createBook(String title, String author, String isbn, int totalCopies) {
        if (totalCopies < 1) {
            throw new BusinessException("Le nombre d'exemplaires doit etre superieur ou egal a 1");
        }
        String normalizedIsbn = isbn.trim();
        if (bookRepository.existsByIsbn(normalizedIsbn)) {
            throw new BusinessException("Un livre avec cet ISBN existe deja : " + normalizedIsbn);
        }
        Book book = new Book(title.trim(), author.trim(), normalizedIsbn, totalCopies);
        Book saved = bookRepository.save(book);
        booksCreatedCounter.increment();
        LOG.info("Book created: id={}, isbn={}, title='{}', copies={}",
                saved.getId(), saved.getIsbn(), saved.getTitle(), saved.getTotalCopies());
        return saved;
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = getBookById(id);
        if (loanRepository.existsByBookIdAndStatus(id, LoanStatus.ACTIVE)) {
            throw new BusinessException("Impossible de supprimer un livre ayant des emprunts actifs");
        }
        bookRepository.delete(book);
        LOG.info("Book deleted: id={}, isbn={}", book.getId(), book.getIsbn());
    }
}
