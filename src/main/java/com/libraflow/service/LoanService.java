package com.libraflow.service;

import com.libraflow.model.AppUser;
import com.libraflow.model.Book;
import com.libraflow.model.Loan;
import com.libraflow.model.LoanStatus;
import com.libraflow.repository.AppUserRepository;
import com.libraflow.repository.BookRepository;
import com.libraflow.repository.LoanRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service handling book borrowing and returning workflows.
 */
@Service
public class LoanService {

    private static final Logger LOG = LoggerFactory.getLogger(LoanService.class);

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final AppUserRepository userRepository;
    private final Counter loansCreatedCounter;
    private final Counter loansReturnedCounter;

    public LoanService(LoanRepository loanRepository,
                       BookRepository bookRepository,
                       AppUserRepository userRepository,
                       MeterRegistry meterRegistry) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.loansCreatedCounter = Counter.builder("libraflow.loans.created")
                .description("Total number of book loans created")
                .register(meterRegistry);
        this.loansReturnedCounter = Counter.builder("libraflow.loans.returned")
                .description("Total number of book loans returned")
                .register(meterRegistry);
    }

    public List<Loan> getAllLoans() {
        return loanRepository.findAllByOrderByBorrowedAtDesc();
    }

    public List<Loan> getLoansForUser(String username) {
        return loanRepository.findByUserUsernameOrderByBorrowedAtDesc(username);
    }

    public long countActiveLoans() {
        return loanRepository.countByStatus(LoanStatus.ACTIVE);
    }

    @Transactional
    public Loan borrowBook(Long bookId, String username) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("Livre introuvable (id=" + bookId + ")"));

        if (!book.isAvailable()) {
            LOG.warn("Borrow rejected: bookId={} has no available copies", bookId);
            throw new BusinessException("Aucun exemplaire disponible pour ce livre");
        }

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable : " + username));

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        Loan loan = new Loan(book, user);
        Loan saved = loanRepository.save(loan);
        loansCreatedCounter.increment();
        LOG.info("Loan created: loanId={}, bookId={}, user='{}', remainingCopies={}",
                saved.getId(), book.getId(), username, book.getAvailableCopies());
        return saved;
    }

    @Transactional
    public Loan returnBook(Long loanId, String actorUsername, boolean isAdmin) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new BusinessException("Emprunt introuvable (id=" + loanId + ")"));

        if (!isAdmin && !loan.getUser().getUsername().equals(actorUsername)) {
            throw new BusinessException("Vous n'etes pas autorise a retourner cet emprunt");
        }

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException("Cet emprunt a deja ete retourne");
        }

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnedAt(Instant.now());

        Book book = loan.getBook();
        book.setAvailableCopies(Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1));
        bookRepository.save(book);

        Loan saved = loanRepository.save(loan);
        loansReturnedCounter.increment();
        LOG.info("Loan returned: loanId={}, bookId={}, actor='{}', availableCopies={}",
                saved.getId(), book.getId(), actorUsername, book.getAvailableCopies());
        return saved;
    }
}
