package com.revhire.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyAnswerRequest {
    @NotBlank @Email private String email;
    @NotBlank private String answer;
}