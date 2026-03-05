package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeAuditTrailResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeEmployeeDetailResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeEmployeeSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationUserResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.IamAuditLogEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.EmployeeProfileEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PersonEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.employee.EmploymentStatus;
import ke.shiva.sbs_iam.modules.iam.domain.enums.party.PartyType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeProfileRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamAuditLogRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrganizationUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import ke.shiva.sbs_iam.modules.reference.domain.entity.BranchEntity;
import ke.shiva.sbs_iam.modules.reference.infra.repository.BranchRepository;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BackofficeEmployeesService {

    private final EmployeeProfileRepository employeeProfileRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final UserContactRepository userContactRepository;
    private final BranchRepository branchRepository;
    private final IamUserRepository iamUserRepository;
    private final IamAuditLogRepository iamAuditLogRepository;

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<BackofficeOrganizationUserResponse> getOrganizationUsersByClientId(String clientId) {
        String normalizedClientId = StringUtils.hasText(clientId) ? clientId.trim() : null;
        if (!StringUtils.hasText(normalizedClientId)) {
            throw BaseException.badRequest("Client ID is required.");
        }

        List<OrganizationUserEntity> organizationUsers =
                organizationUserRepository.findByOrganizationParty_CoreCustomerIdOrderByCreatedAtDesc(normalizedClientId);

        return organizationUsers.stream()
                .map(this::toOrganizationUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BackofficeEmployeeDetailResponse getEmployee(String clientId) {
        EmployeeProfileEntity profile = getRequiredEmployeeProfile(clientId);
        return toDetailResponse(profile);
    }

    @Transactional(readOnly = true)
    public List<BackofficeAuditTrailResponse> getEmployeeAuditTrail(String clientId) {
        EmployeeProfileEntity profile = getRequiredEmployeeProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null || iamUser.getId() == null) {
            return List.of();
        }

        return iamAuditLogRepository.findTop100ByIamUser_IdOrderByCreatedAtDesc(iamUser.getId()).stream()
                .map(this::toAuditTrailResponse)
                .toList();
    }

    @Transactional
    public BackofficeEmployeeDetailResponse updateEmployeeStatus(String clientId, IamStatus status) {
        if (status == null) {
            throw BaseException.badRequest("status is required.");
        }

        EmployeeProfileEntity profile = getRequiredEmployeeProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null) {
            throw BaseException.notFound("IAM user not found for employee.");
        }

        iamUser.setStatus(status);
        iamUser.setUpdatedAt(OffsetDateTime.now());
        iamUserRepository.save(iamUser);

        return toDetailResponse(profile);
    }

    private BackofficeEmployeeSummaryResponse toResponse(EmployeeProfileEntity profile, Map<Long, String> branchNamesById) {
        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;

        String mobile = resolvePrimaryContact(iamUser, ContactType.PHONE);
        String email = resolvePrimaryContact(iamUser, ContactType.EMAIL);

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

    private BackofficeEmployeeDetailResponse toDetailResponse(EmployeeProfileEntity profile) {
        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;

        Map<Long, String> branchNameMap = branchRepository.findAllById(
                profile.getBranch() == null ? Set.<Long>of() : Set.of(profile.getBranch())
        ).stream().collect(Collectors.toMap(BranchEntity::getId, BranchEntity::getBranchName, (existing, ignored) -> existing));

        return BackofficeEmployeeDetailResponse.builder()
                .iamUserId(iamUser != null ? iamUser.getId() : null)
                .clientId(party != null ? party.getCoreCustomerId() : null)
                .fullName(person != null ? person.getFullName() : null)
                .firstName(person != null ? person.getFirstName() : null)
                .lastName(person != null ? person.getLastName() : null)
                .nationalId(person != null ? person.getNationalId() : null)
                .dateOfBirth(person != null ? person.getDob() : null)
                .gender(person != null ? person.getGender() : null)
                .city(person != null ? person.getCity() : null)
                .address(person != null ? person.getAddress() : null)
                .countryCode(person != null && person.getCountryCode() != null ? person.getCountryCode().getCountryCode() : null)
                .mobile(resolvePrimaryContact(iamUser, ContactType.PHONE))
                .email(resolvePrimaryContact(iamUser, ContactType.EMAIL))
                .status(iamUser != null && iamUser.getStatus() != null ? iamUser.getStatus().name() : null)
                .staffNo(profile.getStaffNo())
                .jobTitle(profile.getJobTitle())
                .department(profile.getDepartment())
                .employmentStatus(profile.getEmploymentStatus() != null ? profile.getEmploymentStatus().name() : null)
                .branchId(profile.getBranch())
                .branchName(profile.getBranch() != null ? branchNameMap.get(profile.getBranch()) : null)
                .lastLoginAt(iamUser != null ? iamUser.getLastLoginAt() : null)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private BackofficeAuditTrailResponse toAuditTrailResponse(IamAuditLogEntity entry) {
        return BackofficeAuditTrailResponse.builder()
                .id(entry.getId())
                .eventType(entry.getEventType())
                .userCategory(entry.getUserCategory())
                .channel(entry.getChannel())
                .ipAddress(entry.getIpAddress())
                .deviceId(entry.getDeviceId())
                .entityType(entry.getEntityType())
                .entityId(entry.getEntityId())
                .createdAt(entry.getCreatedAt())
                .metadata(entry.getMetadata())
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

    private EmployeeProfileEntity getRequiredEmployeeProfile(String clientId) {
        String normalizedClientId = StringUtils.hasText(clientId) ? clientId.trim() : null;
        if (!StringUtils.hasText(normalizedClientId)) {
            throw BaseException.badRequest("Client ID is required.");
        }

        EmployeeProfileEntity profile = employeeProfileRepository.findFirstByIamUser_Party_CoreCustomerId(normalizedClientId)
                .orElseThrow(() -> BaseException.notFound("Employee " + normalizedClientId + " not found."));

        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        if (party == null || party.getPartyType() != PartyType.PERSON) {
            throw BaseException.notFound("Employee " + normalizedClientId + " is not a person profile.");
        }

        return profile;
    }

    private String resolvePrimaryContact(IamUserEntity iamUser, ContactType type) {
        if (iamUser == null || type == null) {
            return null;
        }
        return userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(iamUser, type)
                .map(UserContact::getContactValue)
                .orElse(null);
    }

    private BackofficeOrganizationUserResponse toOrganizationUserResponse(OrganizationUserEntity entity) {
        IamUserEntity iamUser = entity.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;

        String mobile = null;
        String email = null;

        if (iamUser != null && iamUser.getContacts() != null) {
            mobile = iamUser.getContacts().stream()
                    .filter(contact -> contact.getContactType() == ContactType.PHONE)
                    .filter(UserContact::isPrimary)
                    .map(UserContact::getContactValue)
                    .findFirst()
                    .orElseGet(() -> iamUser.getContacts().stream()
                            .filter(contact -> contact.getContactType() == ContactType.PHONE)
                            .map(UserContact::getContactValue)
                            .findFirst()
                            .orElse(null));

            email = iamUser.getContacts().stream()
                    .filter(contact -> contact.getContactType() == ContactType.EMAIL)
                    .filter(UserContact::isPrimary)
                    .map(UserContact::getContactValue)
                    .findFirst()
                    .orElseGet(() -> iamUser.getContacts().stream()
                            .filter(contact -> contact.getContactType() == ContactType.EMAIL)
                            .map(UserContact::getContactValue)
                            .findFirst()
                            .orElse(null));
        }

        String roleName = entity.getOrgRole() != null ? entity.getOrgRole().getName() : null;
        String taskRole = entity.getOrgRole() != null && entity.getOrgRole().getTaskRole() != null
                ? entity.getOrgRole().getTaskRole().name()
                : null;

        return BackofficeOrganizationUserResponse.builder()
                .organizationUserId(entity.getId())
                .iamUserId(iamUser != null ? iamUser.getId() : null)
                .clientId(entity.getOrganizationParty() != null ? entity.getOrganizationParty().getCoreCustomerId() : null)
                .fullName(person != null ? person.getFullName() : null)
                .mobile(mobile)
                .email(email)
                .roleName(roleName)
                .taskRole(taskRole)
                .primary(entity.getIsPrimary())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
