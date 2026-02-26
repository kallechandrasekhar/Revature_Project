package com.revhire.controller.jobseeker;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.revhire.dto.ApiResponse;
import com.revhire.dto.jobseeker.*;
import com.revhire.entity.JobSeekerProfile;
import com.revhire.entity.User;
import com.revhire.exception.NotFoundException;
import com.revhire.exception.UnauthorizedException;
import com.revhire.repository.JobSeekerProfileRepository;
import com.revhire.repository.UserRepository;
import com.revhire.service.JobSeekerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/jobseekers")
@RequiredArgsConstructor
@Getter
@Setter
public class JobSeekerController {

    private final JobSeekerService jobSeekerService;
    private final UserRepository userRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;

    @PostMapping("/profile")
    public ApiResponse<JobSeekerProfileDto> createProfile(@AuthenticationPrincipal UserDetails currentUser,
                                                          @Valid @RequestBody CreateJobSeekerProfileRequest request) {
        JobSeekerProfileDto profile = jobSeekerService.createProfile(currentUser, request);
        return ApiResponse.success("Profile created successfully", profile);
    }

    @GetMapping("/profile/me")
    public ApiResponse<JobSeekerProfileDto> getMyProfile(@AuthenticationPrincipal UserDetails currentUser) {
        JobSeekerProfileDto profile = jobSeekerService.getMyProfile(currentUser);
        return ApiResponse.success("Profile retrieved", profile);
    }

    @PutMapping("/profile")
    public ApiResponse<JobSeekerProfileDto> updateProfile(@AuthenticationPrincipal UserDetails currentUser,
                                                          @Valid @RequestBody CreateJobSeekerProfileRequest request) {
        JobSeekerProfileDto profile = jobSeekerService.updateProfile(currentUser, request);
        return ApiResponse.success("Profile updated", profile);
    }

    @PostMapping("/resume/text")
    public ApiResponse<ResumeTextDto> createOrUpdateResumeText(@AuthenticationPrincipal UserDetails currentUser,
                                                               @Valid @RequestBody UpdateResumeTextRequest request) {
        ResumeTextDto resumeText = jobSeekerService.createOrUpdateResumeText(currentUser, request);
        return ApiResponse.success("Resume text saved", resumeText);
    }

    @GetMapping("/resume/text/me")
    public ApiResponse<ResumeTextDto> getMyResumeText(@AuthenticationPrincipal UserDetails currentUser) {
        ResumeTextDto resumeText = jobSeekerService.getMyResumeText(currentUser);
        return ApiResponse.success("Resume text retrieved", resumeText);
    }

    @PostMapping(value = "/resume/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResumeFileDto> uploadResumeFile(@AuthenticationPrincipal UserDetails currentUser,
                                                       @RequestParam("file") MultipartFile file) throws IOException {
        ResumeFileDto resumeFile = jobSeekerService.uploadResumeFile(currentUser, file);
        return ApiResponse.success("Resume file uploaded", resumeFile);
    }

    @GetMapping("/resume/file/me")
    public ApiResponse<ResumeFileDto> getMyResumeFile(@AuthenticationPrincipal UserDetails currentUser) {
        ResumeFileDto resumeFile = jobSeekerService.getMyResumeFile(currentUser);
        return ApiResponse.success("Resume file info retrieved", resumeFile);
    }
    @GetMapping("/profile/by-profile/{profileId}")
    @PreAuthorize("hasRole('EMPLOYER') or hasRole('ADMIN')")
    public ApiResponse<JobSeekerProfileDto> getSeekerProfileByProfileId(@PathVariable Long profileId) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Job seeker profile not found"));
        return ApiResponse.success("Profile retrieved", mapToDto(profile));
    }
    @GetMapping("/resume/file/{profileId}")
    @PreAuthorize("hasRole('EMPLOYER') or hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadResumeFile(@PathVariable Long profileId) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Job seeker profile not found"));
        com.revhire.entity.ResumeFile resumeFile = profile.getResumeFile();
        if (resumeFile == null) {
            throw new NotFoundException("Resume file not found for this profile");
        }
        try {
            Path filePath = Paths.get(resumeFile.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resumeFile.getFileName() + "\"")
                        .contentType(MediaType.parseMediaType(resumeFile.getFileType()))
                        .body(resource);
            } else {
                throw new NotFoundException("File not found on server");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading file", e);
        }
    }

    @GetMapping("/profile/{userId}")
    public ApiResponse<JobSeekerProfileDto> getSeekerProfileByUserId(@PathVariable Long userId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isEmployerOrAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYER") || a.getAuthority().equals("ROLE_ADMIN"));
        if (!isEmployerOrAdmin) {
            throw new UnauthorizedException("Access denied. Only employers can view seeker profiles.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        JobSeekerProfile profile = jobSeekerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Job seeker profile not found"));
        return ApiResponse.success("Profile retrieved", mapToDto(profile));
    }

    private JobSeekerProfileDto mapToDto(JobSeekerProfile profile) {
        JobSeekerProfileDto dto = new JobSeekerProfileDto();
        dto.setId(profile.getId());
        dto.setUserId(profile.getUser().getId());
        dto.setName(profile.getUser().getName());
        dto.setEmail(profile.getUser().getEmail());
        dto.setPhone(profile.getUser().getPhone());
        dto.setLocation(profile.getUser().getLocation());
        dto.setEmploymentStatus(profile.getEmploymentStatus());
        dto.setHeadline(profile.getHeadline());
        dto.setSummary(profile.getSummary());

        if (profile.getResumeText() != null) {
            ResumeTextDto textDto = new ResumeTextDto();
            textDto.setId(profile.getResumeText().getId());
            textDto.setObjective(profile.getResumeText().getObjective());
            textDto.setEducation(profile.getResumeText().getEducation());
            textDto.setExperience(profile.getResumeText().getExperience());
            textDto.setSkills(profile.getResumeText().getSkills());
            textDto.setProjects(profile.getResumeText().getProjects());
            textDto.setCertifications(profile.getResumeText().getCertifications());
            dto.setResumeText(textDto);
        }


        if (profile.getResumeFile() != null) {
            ResumeFileDto fileDto = new ResumeFileDto();
            fileDto.setId(profile.getResumeFile().getId());
            fileDto.setFileName(profile.getResumeFile().getFileName());
            fileDto.setFileType(profile.getResumeFile().getFileType());
            fileDto.setFileSize(profile.getResumeFile().getFileSize());
            fileDto.setUploadDate(profile.getResumeFile().getUploadDate());
            dto.setResumeFile(fileDto);
        }

        return dto;
    }
}