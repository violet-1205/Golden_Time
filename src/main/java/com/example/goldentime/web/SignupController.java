package com.example.goldentime.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.goldentime.auth.UserRegistrationService;
import com.example.goldentime.auth.dto.SignupForm;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/signup")
public class SignupController {

    private final UserRegistrationService userRegistrationService;

    public SignupController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @GetMapping
    public String signupForm(Model model) {
        model.addAttribute("form", new SignupForm());
        return "signup";
    }

    @PostMapping
    public String signup(
            @Valid @ModelAttribute("form") SignupForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("form", form);
            return "signup";
        }

        try {
            userRegistrationService.registerUser(form);
            return "redirect:/login?signupSuccess";
        } catch (IllegalArgumentException e) {
            model.addAttribute("form", form);
            model.addAttribute("errorMessage", e.getMessage());
            return "signup";
        }
    }
}
