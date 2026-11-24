package com.libraflow.controller;

import com.libraflow.model.Book;
import com.libraflow.service.BookService;
import com.libraflow.service.BusinessException;
import com.libraflow.service.LoanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
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
 * Web UI controller for viewing, searching, adding, and deleting books.
 */
@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final LoanService loanService;

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    public BookController(BookService bookService, LoanService loanService) {
        this.bookService = bookService;
        this.loanService = loanService;
    }

    @GetMapping
    public String listBooks(@RequestParam(required = false) String query,
                            Authentication authentication,
                            Model model) {
        List<Book> books = bookService.searchBooks(query);
        model.addAttribute("books", books);
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("totalBooks", books.size());
        model.addAttribute("activeLoans", loanService.countActiveLoans());
        model.addAttribute("username", authentication.getName());
        model.addAttribute("appVersion", appVersion);
        return "books";
    }

    @PostMapping("/add")
    public String addBook(@RequestParam String title,
                          @RequestParam String author,
                          @RequestParam String isbn,
                          @RequestParam int totalCopies,
                          RedirectAttributes redirectAttributes) {
        try {
            bookService.createBook(title, author, isbn, totalCopies);
            redirectAttributes.addFlashAttribute("successMessage", "Livre ajoute au catalogue.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/books";
    }

    @PostMapping("/{id}/delete")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBook(id);
            redirectAttributes.addFlashAttribute("successMessage", "Livre supprime du catalogue.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/books";
    }
}
