package ke.shiva.sbs_iam.modules.reference.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.EmployeeProfileEntity;
import ke.shiva.sbs_iam.modules.reference.domain.enums.BranchTypeEnum;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "branches", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class BranchEntity extends BaseEntity {
    @Size(max = 20)
    @NotNull
    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;

    @Size(max = 255)
    @NotNull
    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'BRANCH'")
    @Column(name = "branch_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private BranchTypeEnum branchTypeEnum;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_code", nullable = false, referencedColumnName = "country_code")
    private CountryEntity countryCode;

    @Size(max = 255)
    @Column(name = "address")
    private String address;

    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "latitude")
    private Double latitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_branch_id")
    private BranchEntity parentBranch;

    @Size(max = 100)
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Size(max = 100)
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Size(max = 100)
    @Column(name = "supervised_by", length = 100)
    private String supervisedBy;

    @Column(name = "supervised_at")
    private OffsetDateTime supervisedAt;

    @OneToMany(mappedBy = "parentBranch")
    private Set<BranchEntity> branches = new LinkedHashSet<>();

    @OneToMany(mappedBy = "branch")
    private Set<EmployeeProfileEntity> employeeProfiles = new LinkedHashSet<>();

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public BranchTypeEnum getBranchType() {
        return branchTypeEnum;
    }

    public void setBranchType(BranchTypeEnum branchTypeEnum) {
        this.branchTypeEnum = branchTypeEnum;
    }

    public CountryEntity getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(CountryEntity countryCode) {
        this.countryCode = countryCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public BranchEntity getParentBranch() {
        return parentBranch;
    }

    public void setParentBranch(BranchEntity parentBranch) {
        this.parentBranch = parentBranch;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getSupervisedBy() {
        return supervisedBy;
    }

    public void setSupervisedBy(String supervisedBy) {
        this.supervisedBy = supervisedBy;
    }

    public OffsetDateTime getSupervisedAt() {
        return supervisedAt;
    }

    public void setSupervisedAt(OffsetDateTime supervisedAt) {
        this.supervisedAt = supervisedAt;
    }

    public Set<BranchEntity> getBranches() {
        return branches;
    }

    public void setBranches(Set<BranchEntity> branches) {
        this.branches = branches;
    }

    public Set<EmployeeProfileEntity> getEmployeeProfiles() {
        return employeeProfiles;
    }

    public void setEmployeeProfiles(Set<EmployeeProfileEntity> employeeProfiles) {
        this.employeeProfiles = employeeProfiles;
    }

}