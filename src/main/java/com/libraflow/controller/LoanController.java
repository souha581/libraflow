package com.libraflow.controller;

import com.libraflow.model.Loan;
import com.libraflow.service.BusinessException;
import com.libraflow.service.LoanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Web UI controller for borrowing and returning books.
 */
@Controller
@RequestMapping("/loans")
public class LoanController {

    private final LoanService loanService;

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public String listLoans(Authentication authentication, Model model) {
        boolean isAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        List<Loan> loans = isAdmin
                ? loanService.getAllLoans()
                : loanService.getLoansForUser(authentication.getName());

        model.addAttribute("loans", loans);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("username", authentication.getName());
        model.addAttribute("appVersion", appVersion);
        return "loans";
    }

    @PostMapping("/borrow")
    public String borrowBook(@RequestParam Long bookId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            loanService.borrowBook(bookId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Emprunt enregistre avec succes.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/books";
    }

    @PostMapping("/{id}/return")
    public String returnBook(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        boolean isAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        try {
            loanService.returnBook(id, authentication.getName(), isAdmin);
            redirectAttributes.addFlashAttribute("successMessage", "Livre retourne avec succes.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/loans";
    }
}
