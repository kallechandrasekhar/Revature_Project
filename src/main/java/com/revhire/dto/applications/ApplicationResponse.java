package com.revhire.dto.applications;

import com.revhire.entity.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;


import java.util.List;

@Data
public class ApplicationResponse {
    private Long id;
    private Long jobPostId;
    private String jobTitle;
    private String companyName;
    private Long jobSeekerId;
    private String jobSeekerName;

    private String education;
    private String experience;
    private List<String> skills;

    private ApplicationStatus status;
    private String coverLetter;
    private LocalDateTime appliedDate;
}