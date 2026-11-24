package com.libraflow.service;

import com.libraflow.model.AppUser;
import com.libraflow.model.Role;
import com.libraflow.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AppUserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, encoder);
    }

    @Test
    void registerMemberHashesPasswordAndNormalizesUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser created = userService.registerMember("  Alice ", "secret123", "Alice Doe");

        assertEquals("alice", created.getUsername());
        assertEquals(Role.MEMBER, created.getRole());
        assertNotEquals("secret123", created.getPasswordHash());
    }

    @Test
    void registerMemberThrowsWhenUsernameTaken() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> userService.registerMember("alice", "secret123", "Alice Doe"));
    }

    @Test
    void registerMemberThrowsWhenPasswordTooShort() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> userService.registerMember("alice", "123", "Alice Doe"));
    }

    @Test
    void findByUsernameReturnsUser() {
        AppUser user = new AppUser("alice", "hash", "Alice", Role.MEMBER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertEquals("alice", userService.findByUsername("alice").getUsername());
    }
}
