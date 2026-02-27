package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.domain.enums.employee.EmploymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeEmployeeOnboardingRequest {
    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotBlank(message = "Staff number is required")
    private String staffNo;

    private String jobTitle;

    private String department;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private EmploymentStatus employmentStatus;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "National ID is required")
    private String nationalId;

    private LocalDate dob;

    private String gender;

    @NotBlank(message = "Mobile number is required")
    private String mobile;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Address is required")
    private String address;

    private String username;

    private List<Long> roleIds;

    private List<@Valid BackofficeAccountRequest> accounts;
}
