package com.libraflow.controller;

import com.libraflow.model.AppUser;
import com.libraflow.model.Book;
import com.libraflow.model.Role;
import com.libraflow.repository.AppUserRepository;
import com.libraflow.repository.BookRepository;
import com.libraflow.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
class WebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Book savedBook;

    @BeforeEach
    void setup() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(new AppUser("admin", passwordEncoder.encode("adminpass"), "Admin", Role.ADMIN));
        userRepository.save(new AppUser("etudiant", passwordEncoder.encode("memberpass"), "Etudiant", Role.MEMBER));
        savedBook = bookRepository.save(new Book("Clean Architecture", "Robert Martin", "978-0134494166", 2));
    }

    @Test
    void loginAndRegisterPagesAreAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void userCanRegisterViaWebForm() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Sami Ben Ali")
                        .param("username", "sami")
                        .param("password", "pass1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void authenticatedMemberCanViewBooksBorrowAndViewLoans() throws Exception {
        mockMvc.perform(get("/books")
                        .with(SecurityMockMvcRequestPostProcessors.user("etudiant").roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(view().name("books"));

        mockMvc.perform(post("/loans/borrow")
                        .with(SecurityMockMvcRequestPostProcessors.user("etudiant").roles("MEMBER"))
                        .with(csrf())
                        .param("bookId", savedBook.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));

        mockMvc.perform(get("/loans")
                        .with(SecurityMockMvcRequestPostProcessors.user("etudiant").roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(view().name("loans"));
    }

    @Test
    void adminCanAddAndDeleteBookViaWebForm() throws Exception {
        mockMvc.perform(post("/books/add")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("title", "Refactoring")
                        .param("author", "Martin Fowler")
                        .param("isbn", "978-0134757599")
                        .param("totalCopies", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));

        mockMvc.perform(post("/books/" + savedBook.getId() + "/delete")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));
    }
}
