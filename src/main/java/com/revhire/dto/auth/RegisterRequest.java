package com.revhire.dto.auth;

import com.revhire.entity.enums.EmploymentStatus;
import com.revhire.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Email private String email;
    @NotBlank private String password;
    @NotBlank private String name;
    private String phone;
    private String location;
    @NotNull private Role role;
    private EmploymentStatus employmentStatus;
    private String companyName;
    private String industry;
    private String website;
    private String companyDescription;
    private String companySize;

    // New fields for security question and answer
    @NotBlank private String securityQuestion;
    @NotBlank private String securityAnswer;
}