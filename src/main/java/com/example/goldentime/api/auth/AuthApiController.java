package com.example.goldentime.api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.goldentime.api.auth.dto.SignupRequest;
import com.example.goldentime.api.auth.dto.SignupResponse;
import com.example.goldentime.auth.UserRegistrationService;
import com.example.goldentime.auth.dto.SignupForm;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final UserRegistrationService userRegistrationService;

    public AuthApiController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Validated @RequestBody SignupRequest request) {
        SignupForm form = new SignupForm();
        form.setLoginId(request.getLoginId());
        form.setPassword(request.getPassword());
        form.setUserName(request.getUserName());
        form.setPhone(request.getPhone());
        form.setEmail(request.getEmail());
        form.setAddress(request.getAddress());
        form.setCarNumber(request.getCarNumber());
        form.setSerialNumber(request.getSerialNumber());

        try {
            Long userId = userRegistrationService.registerUser(form).getUserId();
            return ResponseEntity.status(HttpStatus.CREATED).body(new SignupResponse(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
