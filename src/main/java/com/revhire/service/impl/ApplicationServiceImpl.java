package com.revhire.service.impl;

import com.revhire.dto.applications.*;
import com.revhire.entity.*;
import com.revhire.entity.enums.ApplicationStatus;
import com.revhire.exception.BadRequestException;
import com.revhire.exception.NotFoundException;
import com.revhire.exception.UnauthorizedException;
import com.revhire.repository.*;
import com.revhire.service.ApplicationService;
import com.revhire.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final UserRepository userRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final JobPostRepository jobPostRepository;
    private final JobApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    private User getUser(UserDetails userDetails) {
        log.debug("Fetching user with email: {}", userDetails.getUsername());
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", userDetails.getUsername());
                    return new NotFoundException("User not found");
                });
    }

    private JobSeekerProfile getJobSeekerProfile(User user) {
        if (user.getJobSeekerProfile() == null) {
            log.warn("Job seeker profile not found for user: {}", user.getEmail());
            throw new BadRequestException("Job seeker profile not found");
        }
        return user.getJobSeekerProfile();
    }

    private EmployerProfile getEmployerProfile(User user) {
        if (user.getEmployerProfile() == null) {
            log.error("Employer profile not found for user: {}", user.getEmail());
            throw new NotFoundException("Employer profile not found");
        }
        return user.getEmployerProfile();
    }

    private JobPost getJobPost(Long jobId) {
        log.debug("Fetching job post with id: {}", jobId);
        return jobPostRepository.findById(jobId)
                .orElseThrow(() -> {
                    log.error("Job post not found with id: {}", jobId);
                    return new NotFoundException("Job post not found");
                });
    }

    private JobApplication getApplication(Long applicationId) {
        log.debug("Fetching application with id: {}", applicationId);
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> {
                    log.error("Application not found with id: {}", applicationId);
                    return new NotFoundException("Application not found");
                });
    }

    private ApplicationResponse mapToDto(JobApplication app) {
        ApplicationResponse dto = new ApplicationResponse();
        dto.setId(app.getId());
        dto.setJobPostId(app.getJobPost().getId());
        dto.setJobTitle(app.getJobPost().getTitle());
        dto.setCompanyName(app.getJobPost().getEmployer().getCompanyName());
        dto.setJobSeekerId(app.getJobSeeker().getId());
        dto.setJobSeekerName(app.getJobSeeker().getUser().getName());
        dto.setStatus(app.getStatus());
        dto.setCoverLetter(app.getCoverLetter());
        dto.setAppliedDate(app.getAppliedDate());

        // ✅ resume fields
        ResumeText rt = app.getJobSeeker().getResumeText();
        if (rt != null) {
            dto.setEducation(rt.getEducation());
            dto.setExperience(rt.getExperience());


            if (rt.getSkills() != null && !rt.getSkills().isBlank()) {
                List<String> skills = List.of(rt.getSkills().split("[,\\n]"))
                        .stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                dto.setSkills(skills);
            } else {
                dto.setSkills(List.of());
            }
        } else {
            dto.setSkills(List.of());
        }

        return dto;
    }

    @Override
    @Transactional
    public ApplicationResponse applyForJob(UserDetails currentUser, ApplyJobRequest request) {

        log.info("User {} attempting to apply for job {}",
                currentUser.getUsername(), request.getJobPostId());

        User user = getUser(currentUser);
        JobSeekerProfile jobSeeker = getJobSeekerProfile(user);
        JobPost jobPost = getJobPost(request.getJobPostId());

        if (applicationRepository.existsByJobPostIdAndJobSeekerId(jobPost.getId(), jobSeeker.getId())) {
            log.warn("User {} already applied for job {}", user.getEmail(), jobPost.getId());
            throw new BadRequestException("Already applied");
        }

        if (!jobPost.getIsActive() || jobPost.getIsFilled()) {
            log.warn("Job {} not accepting applications", jobPost.getId());
            throw new BadRequestException("Job not accepting applications");
        }

        if (jobPost.getApplicationDeadline() != null &&
                jobPost.getApplicationDeadline().isBefore(LocalDateTime.now().toLocalDate())) {
            log.warn("Application deadline passed for job {}", jobPost.getId());
            throw new BadRequestException("Deadline passed");
        }

        JobApplication application = JobApplication.builder()
                .jobPost(jobPost)
                .jobSeeker(jobSeeker)
                .status(ApplicationStatus.APPLIED)
                .coverLetter(request.getCoverLetter())
                .appliedDate(LocalDateTime.now())
                .build();

        application = applicationRepository.save(application);

        log.info("Application {} created successfully for job {}",
                application.getId(), jobPost.getId());

        notificationService.createNotification(
                jobPost.getEmployer().getUser(),
                "New application for " + jobPost.getTitle(),
                "APPLICATION_UPDATE",
                "/employer/jobs/" + jobPost.getId() + "/applications"
        );

        return mapToDto(application);
    }

    @Override
    public List<ApplicationResponse> getMyApplications(UserDetails currentUser) {

        log.info("Fetching applications for user {}", currentUser.getUsername());

        User user = getUser(currentUser);
        JobSeekerProfile jobSeeker = getJobSeekerProfile(user);

        List<ApplicationResponse> responses = applicationRepository
                .findByJobSeekerId(jobSeeker.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        log.info("User {} has {} applications",
                user.getEmail(), responses.size());

        return responses;
    }

    @Override
    @Transactional
    public void withdrawApplication(UserDetails currentUser, Long applicationId, WithdrawApplicationRequest request) {

        log.info("User {} attempting to withdraw application {}",
                currentUser.getUsername(), applicationId);

        User user = getUser(currentUser);
        JobSeekerProfile jobSeeker = getJobSeekerProfile(user);
        JobApplication application = getApplication(applicationId);

        if (!application.getJobSeeker().getId().equals(jobSeeker.getId())) {
            log.warn("Unauthorized withdraw attempt by user {} for application {}",
                    user.getEmail(), applicationId);
            throw new UnauthorizedException("Not authorized");
        }

        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            log.warn("Application {} already withdrawn", applicationId);
            throw new BadRequestException("Already withdrawn");
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.save(application);

        log.info("Application {} withdrawn successfully", applicationId);

        notificationService.createNotification(
                application.getJobPost().getEmployer().getUser(),
                "Application withdrawn by " + user.getName(),
                "APPLICATION_UPDATE",
                "/employer/jobs/" + application.getJobPost().getId() + "/applications"
        );
    }

    @Override
    public List<ApplicationResponse> getApplicationsForJob(UserDetails currentUser, Long jobId) {

        log.info("Employer {} fetching applications for job {}",
                currentUser.getUsername(), jobId);

        User user = getUser(currentUser);
        EmployerProfile employer = getEmployerProfile(user);
        JobPost jobPost = getJobPost(jobId);

        if (!jobPost.getEmployer().getId().equals(employer.getId())) {
            log.warn("Unauthorized access attempt for job {} by employer {}",
                    jobId, user.getEmail());
            throw new UnauthorizedException("Not authorized");
        }

        return applicationRepository.findByJobPostId(jobId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApplicationResponse updateApplicationStatus(
            UserDetails currentUser,
            Long applicationId,
            UpdateApplicationStatusRequest request) {

        log.info("Employer {} updating status of application {} to {}",
                currentUser.getUsername(), applicationId, request.getStatus());

        User user = getUser(currentUser);
        EmployerProfile employer = getEmployerProfile(user);
        JobApplication application = getApplication(applicationId);

        if (!application.getJobPost().getEmployer().getId().equals(employer.getId())) {
            log.warn("Unauthorized status update attempt for application {} by {}",
                    applicationId, user.getEmail());
            throw new UnauthorizedException("Not authorized");
        }

        application.setStatus(request.getStatus());

        if (request.getEmployerNotes() != null)
            application.setEmployerNotes(request.getEmployerNotes());

        application = applicationRepository.save(application);

        log.info("Application {} status updated successfully", applicationId);

        notificationService.createNotification(
                application.getJobSeeker().getUser(),
                "Your application for " + application.getJobPost().getTitle()
                        + " is now " + request.getStatus(),
                "APPLICATION_UPDATE",
                "/jobseeker/applications"
        );

        return mapToDto(application);
    }

    @Override
    @Transactional
    public ApplicationResponse addNote(
            UserDetails currentUser,
            Long applicationId,
            NoteRequest request) {

        log.info("Employer {} adding note to application {}",
                currentUser.getUsername(), applicationId);

        User user = getUser(currentUser);
        EmployerProfile employer = getEmployerProfile(user);
        JobApplication application = getApplication(applicationId);

        if (!application.getJobPost().getEmployer().getId().equals(employer.getId())) {
            log.warn("Unauthorized note addition attempt for application {} by {}",
                    applicationId, user.getEmail());
            throw new UnauthorizedException("Not authorized");
        }

        application.setEmployerNotes(request.getNote());
        application = applicationRepository.save(application);

        log.info("Note added successfully to application {}", applicationId);

        return mapToDto(application);
    }
}