package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_profile", schema = "iam_service")
public class CustomerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "iam_user_id", nullable = false)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iam_user_id", nullable = false)
    private IamUser iamUser;

    @Size(max = 50)
    @Column(name = "core_customer_id", length = 50)
    private String coreCustomerId;

    @Size(max = 50)
    @Column(name = "segment", length = 50)
    private String segment;

    @Size(max = 10)
    @ColumnDefault("'en'")
    @Column(name = "language", length = 10)
    private String language;

    @Size(max = 50)
    @ColumnDefault("'Africa/Nairobi'")
    @Column(name = "timezone", length = 50)
    private String timezone;

    @Size(max = 20)
    @ColumnDefault("'light'")
    @Column(name = "theme", length = 20)
    private String theme;

    @ColumnDefault("true")
    @Column(name = "allow_email")
    private Boolean allowEmail;

    @ColumnDefault("true")
    @Column(name = "allow_sms")
    private Boolean allowSms;

    @ColumnDefault("false")
    @Column(name = "allow_push")
    private Boolean allowPush;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IamUser getIamUser() {
        return iamUser;
    }

    public void setIamUser(IamUser iamUser) {
        this.iamUser = iamUser;
    }

    public String getCoreCustomerId() {
        return coreCustomerId;
    }

    public void setCoreCustomerId(String coreCustomerId) {
        this.coreCustomerId = coreCustomerId;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public Boolean getAllowEmail() {
        return allowEmail;
    }

    public void setAllowEmail(Boolean allowEmail) {
        this.allowEmail = allowEmail;
    }

    public Boolean getAllowSms() {
        return allowSms;
    }

    public void setAllowSms(Boolean allowSms) {
        this.allowSms = allowSms;
    }

    public Boolean getAllowPush() {
        return allowPush;
    }

    public void setAllowPush(Boolean allowPush) {
        this.allowPush = allowPush;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}