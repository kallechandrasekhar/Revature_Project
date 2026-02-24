package com.revhire.service.impl;

import com.revhire.dto.auth.*;
import com.revhire.entity.*;
import com.revhire.entity.enums.Role;
import com.revhire.exception.BadRequestException;
import com.revhire.repository.*;
import com.revhire.security.JwtService;
import com.revhire.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email already registered: {}", request.getEmail());
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .location(request.getLocation())
                .role(request.getRole())
                .build();

        if (request.getSecurityQuestion() != null && request.getSecurityAnswer() != null) {
            user.setSecurityQuestion(request.getSecurityQuestion());
            user.setSecurityAnswer(passwordEncoder.encode(request.getSecurityAnswer()));
        }

        user = userRepository.save(user);
        log.info("User registered successfully with id {} and role {}", user.getId(), user.getRole());

        if (request.getRole() == Role.JOB_SEEKER) {
            log.debug("Creating JobSeeker profile for user {}", user.getEmail());
            JobSeekerProfile profile = JobSeekerProfile.builder()
                    .user(user)
                    .employmentStatus(request.getEmploymentStatus())
                    .build();
            jobSeekerProfileRepository.save(profile);
            log.info("JobSeeker profile created for user {}", user.getEmail());
        } else if (request.getRole() == Role.EMPLOYER) {
            log.debug("Creating Employer profile for user {}", user.getEmail());
            EmployerProfile profile = EmployerProfile.builder()
                    .user(user)
                    .companyName(request.getCompanyName())
                    .industry(request.getIndustry())
                    .companySize(request.getCompanySize())
                    .companyDescription(request.getCompanyDescription())
                    .website(request.getWebsite())
                    .location(request.getLocation())
                    .build();
            employerProfileRepository.save(profile);
            log.info("Employer profile created for user {}", user.getEmail());
        }

        String token = jwtService.generateToken(user);
        log.info("JWT token generated for user {}", user.getEmail());

        return new AuthResponse(token, user.getEmail(), user.getRole(), user.getName());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        log.debug("Authentication successful for email: {}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("Login failed — user not found: {}", request.getEmail());
                    return new UsernameNotFoundException("User not found");
                });
        String token = jwtService.generateToken(user);
        log.info("User {} logged in successfully", user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getRole(), user.getName());
    }

    @Override
    public String getSecurityQuestion(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (user.getSecurityQuestion() == null) {
            throw new BadRequestException("Security question not set for this user");
        }
        return user.getSecurityQuestion();
    }

    @Override
    public String verifyAnswerAndGenerateToken(String email, String answer) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (user.getSecurityAnswer() == null) {
            throw new BadRequestException("Security answer not set");
        }
        if (!passwordEncoder.matches(answer, user.getSecurityAnswer())) {
            throw new BadRequestException("Incorrect answer");
        }
        return jwtService.generateResetToken(email);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        if (!jwtService.isResetTokenValid(token)) {
            throw new BadRequestException("Invalid or expired token");
        }
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}