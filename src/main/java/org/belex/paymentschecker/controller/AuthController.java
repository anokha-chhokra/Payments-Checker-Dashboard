package org.belex.paymentschecker.controller;

import org.belex.paymentschecker.modal.AppUser;
import org.belex.paymentschecker.repo.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String email, @RequestParam String password, Model model) {
        if (email == null || email.isBlank() || password == null || password.length() < 6) {
            model.addAttribute("error", "Please provide an email and a password of at least 6 characters.");
            return "signup";
        }
        if (appUserRepository.findByEmail(email).isPresent()) {
            model.addAttribute("error", "An account with that email already exists.");
            return "signup";
        }
        AppUser user = new AppUser();
        user.setEmail(email.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        appUserRepository.save(user);
        model.addAttribute("created", true);
        return "login";
    }
}
