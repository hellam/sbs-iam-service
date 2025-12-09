package ke.shiva.sbs_iam.modules.reference.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PersonEntity;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "countries", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class CountryEntity extends BaseEntity {
    @Size(max = 3)
    @NotNull
    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode;

    @Size(max = 5)
    @NotNull
    @Column(name = "phone_code", nullable = false, length = 5)
    private String phoneCode;

    @Size(max = 100)
    @NotNull
    @Column(name = "country_name", nullable = false, length = 100)
    private String countryName;

    @Size(max = 3)
    @NotNull
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Size(max = 100)
    @NotNull
    @Column(name = "currency_name", nullable = false, length = 100)
    private String currencyName;

    @OneToMany(mappedBy = "countryCode")
    private Set<BranchEntity> branches = new LinkedHashSet<>();

    @OneToMany(mappedBy = "countryCode")
    private Set<OrganizationEntity> organizations = new LinkedHashSet<>();

    @OneToMany(mappedBy = "countryCode")
    private Set<PersonEntity> people = new LinkedHashSet<>();

}