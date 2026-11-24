package com.libraflow.service;

import com.libraflow.model.AppUser;
import com.libraflow.model.Role;
import com.libraflow.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling user registration and lookup.
 */
@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser registerMember(String username, String rawPassword, String fullName) {
        String normalized = username.trim().toLowerCase();
        if (userRepository.existsByUsername(normalized)) {
            throw new BusinessException("Ce nom d'utilisateur est deja utilise : " + normalized);
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new BusinessException("Le mot de passe doit contenir au moins 6 caracteres");
        }
        AppUser user = new AppUser(normalized, passwordEncoder.encode(rawPassword), fullName.trim(), Role.MEMBER);
        AppUser saved = userRepository.save(user);
        LOG.info("Member registered: username='{}'", saved.getUsername());
        return saved;
    }

    public AppUser findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable : " + username));
    }
}
