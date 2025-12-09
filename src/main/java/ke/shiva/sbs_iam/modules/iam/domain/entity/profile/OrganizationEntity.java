package ke.shiva.sbs_iam.modules.iam.domain.entity.profile;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Setter
@Getter
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

}