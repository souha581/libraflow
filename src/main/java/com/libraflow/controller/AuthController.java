package com.libraflow.controller;

import com.libraflow.service.BusinessException;
import com.libraflow.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles root redirect, login, and member self-registration pages.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/books";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String fullName,
                           @RequestParam String password,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            userService.registerMember(username, password, fullName);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Compte cree avec succes. Vous pouvez maintenant vous connecter.");
            return "redirect:/login";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        }
    }
}
