package com.libraflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraflow.model.AppUser;
import com.libraflow.model.Role;
import com.libraflow.repository.AppUserRepository;
import com.libraflow.repository.BookRepository;
import com.libraflow.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetDatabase() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(new AppUser("admin", passwordEncoder.encode("testpass1"), "Admin", Role.ADMIN));
        userRepository.save(new AppUser("member", passwordEncoder.encode("testpass2"), "Member", Role.MEMBER));
    }

    @Test
    void actuatorHealthEndpointIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void memberCannotCreateBookWhileAdminCanBorrowAndReturnFlowWorks() throws Exception {
        String createPayload = """
                {
                  "title": "Site Reliability Engineering",
                  "author": "Betsy Beyer",
                  "isbn": "978-1491929124",
                  "totalCopies": 2
                }
                """;

        // Member is forbidden from creating books
        mockMvc.perform(post("/api/books")
                        .with(SecurityMockMvcRequestPostProcessors.user("member").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isForbidden());

        // Admin creates a book
        MvcResult createResult = mockMvc.perform(post("/api/books")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availableCopies").value(2))
                .andReturn();

        JsonNode createdBook = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long bookId = createdBook.get("id").asLong();

        // Public catalog lists the book
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isbn").value("978-1491929124"));

        // Member borrows the book
        MvcResult borrowResult = mockMvc.perform(post("/api/loans/borrow")
                        .with(SecurityMockMvcRequestPostProcessors.user("member").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": " + bookId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        long loanId = objectMapper.readTree(borrowResult.getResponse().getContentAsString()).get("id").asLong();

        // Available copies decreased to 1
        mockMvc.perform(get("/api/books/" + bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(1));

        // Member returns the book
        mockMvc.perform(post("/api/loans/" + loanId + "/return")
                        .with(SecurityMockMvcRequestPostProcessors.user("member").roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        // Available copies restored to 2
        mockMvc.perform(get("/api/books/" + bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(2));
    }
}
