package com.libraflow.repository;

import com.libraflow.model.Loan;
import com.libraflow.model.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA repository for book borrowing transactions.
 */
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserUsernameOrderByBorrowedAtDesc(String username);

    List<Loan> findAllByOrderByBorrowedAtDesc();

    long countByStatus(LoanStatus status);

    boolean existsByBookIdAndStatus(Long bookId, LoanStatus status);
}
