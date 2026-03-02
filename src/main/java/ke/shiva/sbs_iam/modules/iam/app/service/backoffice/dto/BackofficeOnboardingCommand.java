package ke.shiva.sbs_iam.modules.iam.app.service.backoffice.dto;

import ke.shiva.sbs_iam.modules.iam.domain.enums.employee.EmploymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeOnboardingCommand {
    private String clientId;

    // Customer-only (optional)
    private List<String> accounts;

    // Employee-only
    private String staffNo;
    private Long branchId;
    private String jobTitle;
    private String department;
    private EmploymentStatus employmentStatus;
    private String username;
    private List<Long> roleIds;

    // Organization-only
    private String registrationNo;
    private Boolean isSme;
}
