package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "client_credentials", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class ClientCredential extends BaseEntity {
    @Size(max = 150)
    @NotNull
    @Column(name = "client_id", nullable = false, length = 150)
    private String clientId;

    @Size(max = 255)
    @NotNull
    @Column(name = "client_secret_hash", nullable = false)
    private String clientSecretHash;

    @Size(max = 128)
    @Column(name = "certificate_fingerprint", length = 128)
    private String certificateFingerprint;

    @Column(name = "scopes", length = Integer.MAX_VALUE)
    private String scopes;

    @Column(name = "allowed_ip_ranges", length = Integer.MAX_VALUE)
    private String allowedIpRanges;

    @ColumnDefault("true")
    @Column(name = "requires_mtls")
    private Boolean requiresMtls;

    @Size(max = 20)
    @ColumnDefault("'jwt'")
    @Column(name = "token_type", length = 20)
    private String tokenType;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecretHash() {
        return clientSecretHash;
    }

    public void setClientSecretHash(String clientSecretHash) {
        this.clientSecretHash = clientSecretHash;
    }

    public String getCertificateFingerprint() {
        return certificateFingerprint;
    }

    public void setCertificateFingerprint(String certificateFingerprint) {
        this.certificateFingerprint = certificateFingerprint;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String getAllowedIpRanges() {
        return allowedIpRanges;
    }

    public void setAllowedIpRanges(String allowedIpRanges) {
        this.allowedIpRanges = allowedIpRanges;
    }

    public Boolean getRequiresMtls() {
        return requiresMtls;
    }

    public void setRequiresMtls(Boolean requiresMtls) {
        this.requiresMtls = requiresMtls;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(OffsetDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

}