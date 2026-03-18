package com.example.goldentime.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @NotBlank
    private String loginId;

    @NotBlank
    @Size(min = 6)
    private String password;

    @NotBlank
    private String userName;

    private String phone;

    @Email
    private String email;

    private String address;

    @NotBlank
    private String carNumber;

    @NotBlank
    private String serialNumber;
}
