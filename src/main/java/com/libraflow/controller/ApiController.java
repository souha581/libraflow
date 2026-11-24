package com.libraflow.controller;

import com.libraflow.model.Book;
import com.libraflow.model.Loan;
import com.libraflow.service.BookService;
import com.libraflow.service.BusinessException;
import com.libraflow.service.LoanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST API endpoints used for programmatic integrations, API tests, and smoke tests.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final BookService bookService;
    private final LoanService loanService;

    public ApiController(BookService bookService, LoanService loanService) {
        this.bookService = bookService;
        this.loanService = loanService;
    }

    @GetMapping("/books")
    public List<BookResponse> getBooks(@RequestParam(required = false) String query) {
        return bookService.searchBooks(query).stream()
                .map(BookResponse::from)
                .toList();
    }

    @GetMapping("/books/{id}")
    public BookResponse getBook(@PathVariable Long id) {
        return BookResponse.from(bookService.getBookById(id));
    }

    @PostMapping("/books")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        Book created = bookService.createBook(
                request.title(),
                request.author(),
                request.isbn(),
                request.totalCopies()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(BookResponse.from(created));
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/loans")
    public List<LoanResponse> getLoans(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        List<Loan> loans = isAdmin
                ? loanService.getAllLoans()
                : loanService.getLoansForUser(authentication.getName());
        return loans.stream().map(LoanResponse::from).toList();
    }

    @PostMapping("/loans/borrow")
    public ResponseEntity<LoanResponse> borrowBook(@Valid @RequestBody BorrowRequest request,
                                                   Authentication authentication) {
        Loan created = loanService.borrowBook(request.bookId(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanResponse.from(created));
    }

    @PostMapping("/loans/{id}/return")
    public LoanResponse returnBook(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        Loan returned = loanService.returnBook(id, authentication.getName(), isAdmin);
        return LoanResponse.from(returned);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    public record CreateBookRequest(
            @NotBlank String title,
            @NotBlank String author,
            @NotBlank String isbn,
            @Min(1) int totalCopies
    ) {
    }

    public record BorrowRequest(@NotNull Long bookId) {
    }

    public record BookResponse(
            Long id,
            String title,
            String author,
            String isbn,
            int totalCopies,
            int availableCopies
    ) {
        static BookResponse from(Book book) {
            return new BookResponse(
                    book.getId(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.getIsbn(),
                    book.getTotalCopies(),
                    book.getAvailableCopies()
            );
        }
    }

    public record LoanResponse(
            Long id,
            Long bookId,
            String bookTitle,
            String username,
            String status,
            Instant borrowedAt,
            Instant returnedAt
    ) {
        static LoanResponse from(Loan loan) {
            return new LoanResponse(
                    loan.getId(),
                    loan.getBook().getId(),
                    loan.getBook().getTitle(),
                    loan.getUser().getUsername(),
                    loan.getStatus().name(),
                    loan.getBorrowedAt(),
                    loan.getReturnedAt()
            );
        }
    }
}
