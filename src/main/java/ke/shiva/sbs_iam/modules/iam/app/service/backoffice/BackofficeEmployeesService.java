package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeEmployeeSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.EmployeeProfileEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PersonEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.employee.EmploymentStatus;
import ke.shiva.sbs_iam.modules.iam.domain.enums.party.PartyType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeProfileRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import ke.shiva.sbs_iam.modules.reference.domain.entity.BranchEntity;
import ke.shiva.sbs_iam.modules.reference.infra.repository.BranchRepository;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BackofficeEmployeesService {

    private final EmployeeProfileRepository employeeProfileRepository;
    private final UserContactRepository userContactRepository;
    private final BranchRepository branchRepository;

    public PaginatedResponse<BackofficeEmployeeSummaryResponse> getEmployees(HttpServletRequest request) {
        validateFilters(request);

        List<String> searchableColumns = List.of(
                "iamUser.party.coreCustomerId",
                "iamUser.party.person.fullName",
                "staffNo",
                "jobTitle",
                "department",
                "iamUser.contacts.contactValue"
        );
        List<String> sortableColumns = List.of(
                "createdAt",
                "updatedAt",
                "staffNo",
                "employmentStatus",
                "branch",
                "iamUser.status",
                "iamUser.party.coreCustomerId",
                "iamUser.party.person.fullName"
        );
        List<String> filterableColumns = List.of(
                "employmentStatus",
                "branch",
                "iamUser.status"
        );

        Page<EmployeeProfileEntity> page = PaginationUtil.filterAndPaginateWithScoping(
                employeeProfileRepository,
                request,
                searchableColumns,
                sortableColumns,
                filterableColumns,
                "iamUser.party.partyType",
                PartyType.PERSON.name(),
                10
        );

        Set<Long> branchIds = page.getContent().stream()
                .map(EmployeeProfileEntity::getBranch)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> branchNamesById = branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(BranchEntity::getId, BranchEntity::getBranchName, (existing, ignored) -> existing));

        Page<BackofficeEmployeeSummaryResponse> dtoPage = page.map(entity -> toResponse(entity, branchNamesById));
        return PaginationUtil.toPaginatedResponse(dtoPage);
    }

    private BackofficeEmployeeSummaryResponse toResponse(EmployeeProfileEntity profile, Map<Long, String> branchNamesById) {
        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;

        String mobile = iamUser != null
                ? userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(iamUser, ContactType.PHONE)
                .map(UserContact::getContactValue)
                .orElse(null)
                : null;

        String email = iamUser != null
                ? userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(iamUser, ContactType.EMAIL)
                .map(UserContact::getContactValue)
                .orElse(null)
                : null;

        return BackofficeEmployeeSummaryResponse.builder()
                .iamUserId(iamUser != null ? iamUser.getId() : null)
                .clientId(party != null ? party.getCoreCustomerId() : null)
                .fullName(person != null ? person.getFullName() : null)
                .staffNo(profile.getStaffNo())
                .mobile(mobile)
                .email(email)
                .jobTitle(profile.getJobTitle())
                .department(profile.getDepartment())
                .employmentStatus(profile.getEmploymentStatus() != null ? profile.getEmploymentStatus().name() : null)
                .branchId(profile.getBranch())
                .branchName(profile.getBranch() != null ? branchNamesById.get(profile.getBranch()) : null)
                .status(iamUser != null && iamUser.getStatus() != null ? iamUser.getStatus().name() : null)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private void validateFilters(HttpServletRequest request) {
        String iamStatus = firstNonBlank(
                request.getParameter("status"),
                request.getParameter("iamUser.status"),
                request.getParameter("iamUser_status")
        );
        if (StringUtils.hasText(iamStatus)) {
            try {
                IamStatus.valueOf(iamStatus.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw BaseException.badRequest("Invalid status. Allowed values: ACTIVE, INACTIVE, LOCKED, SUSPENDED.");
            }
        }

        String employmentStatus = firstNonBlank(request.getParameter("employmentStatus"));
        if (StringUtils.hasText(employmentStatus)) {
            try {
                EmploymentStatus.valueOf(employmentStatus.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw BaseException.badRequest("Invalid employmentStatus. Allowed values: ACTIVE, SUSPENDED, TERMINATED, ON_LEAVE.");
            }
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
