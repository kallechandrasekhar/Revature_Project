package com.revhire.controller;

import com.revhire.dto.ApiResponse;
import com.revhire.dto.auth.*;
import com.revhire.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.success("Registration successful", response);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success("Login successful", response);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<SecurityQuestionResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String question = authService.getSecurityQuestion(request.getEmail());
        return ApiResponse.success("Security question retrieved", new SecurityQuestionResponse(question));
    }

    @PostMapping("/verify-answer")
    public ApiResponse<VerifyAnswerResponse> verifyAnswer(@Valid @RequestBody VerifyAnswerRequest request) {
        String token = authService.verifyAnswerAndGenerateToken(request.getEmail(), request.getAnswer());
        return ApiResponse.success("Answer verified", new VerifyAnswerResponse(token));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.success("Password reset successfully", null);
    }
}