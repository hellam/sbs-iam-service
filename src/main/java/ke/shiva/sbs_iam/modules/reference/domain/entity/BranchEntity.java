package ke.shiva.sbs_iam.modules.reference.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.reference.domain.enums.BranchTypeEnum;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
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
}