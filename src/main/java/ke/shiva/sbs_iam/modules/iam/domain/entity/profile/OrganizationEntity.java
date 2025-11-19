package ke.shiva.sbs_iam.modules.iam.domain.entity.profile;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "organization", schema = "iam_service")
public class OrganizationEntity {
    @Id
    @Column(name = "party_id", nullable = false)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id", nullable = false)
    private PartyEntity party;

    @Size(max = 255)
    @NotNull
    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Size(max = 255)
    @Column(name = "display_name")
    private String displayName;

    @Size(max = 100)
    @Column(name = "registration_no", length = 100)
    private String registrationNo;

    @Size(max = 50)
    @NotNull
    @Column(name = "customer_segment", nullable = false, length = 50)
    private String customerSegment;

    @ColumnDefault("false")
    @Column(name = "sme_mode")
    private Boolean smeMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_code", referencedColumnName = "country_code")
    private CountryEntity countryCode;

    @Size(max = 255)
    @Column(name = "address")
    private String address;

    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;

    @Size(max = 20)
    @Column(name = "company_phone", length = 20)
    private String companyPhone;

    @Size(max = 255)
    @Column(name = "company_email")
    private String companyEmail;

    @Size(max = 200)
    @Column(name = "contact_person_name", length = 200)
    private String contactPersonName;

    @Size(max = 255)
    @Column(name = "contact_person_email")
    private String contactPersonEmail;

    @Size(max = 20)
    @Column(name = "contact_person_phone", length = 20)
    private String contactPersonPhone;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PartyEntity getParty() {
        return party;
    }

    public void setParty(PartyEntity party) {
        this.party = party;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public void setRegistrationNo(String registrationNo) {
        this.registrationNo = registrationNo;
    }

    public String getCustomerSegment() {
        return customerSegment;
    }

    public void setCustomerSegment(String customerSegment) {
        this.customerSegment = customerSegment;
    }

    public Boolean getSmeMode() {
        return smeMode;
    }

    public void setSmeMode(Boolean smeMode) {
        this.smeMode = smeMode;
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

    public String getCompanyPhone() {
        return companyPhone;
    }

    public void setCompanyPhone(String companyPhone) {
        this.companyPhone = companyPhone;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public void setCompanyEmail(String companyEmail) {
        this.companyEmail = companyEmail;
    }

    public String getContactPersonName() {
        return contactPersonName;
    }

    public void setContactPersonName(String contactPersonName) {
        this.contactPersonName = contactPersonName;
    }

    public String getContactPersonEmail() {
        return contactPersonEmail;
    }

    public void setContactPersonEmail(String contactPersonEmail) {
        this.contactPersonEmail = contactPersonEmail;
    }

    public String getContactPersonPhone() {
        return contactPersonPhone;
    }

    public void setContactPersonPhone(String contactPersonPhone) {
        this.contactPersonPhone = contactPersonPhone;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}