package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "organization_approver_secret_history", schema = "iam_service")
public class OrganizationApproverSecretHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_user_id", nullable = false)
    private OrganizationUser organizationUser;

    @Size(max = 255)
    @Column(name = "approver_password_hash")
    private String approverPasswordHash;

    @Size(max = 255)
    @Column(name = "approver_pin_hash")
    private String approverPinHash;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrganizationUser getOrganizationUser() {
        return organizationUser;
    }

    public void setOrganizationUser(OrganizationUser organizationUser) {
        this.organizationUser = organizationUser;
    }

    public String getApproverPasswordHash() {
        return approverPasswordHash;
    }

    public void setApproverPasswordHash(String approverPasswordHash) {
        this.approverPasswordHash = approverPasswordHash;
    }

    public String getApproverPinHash() {
        return approverPinHash;
    }

    public void setApproverPinHash(String approverPinHash) {
        this.approverPinHash = approverPinHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

}