package ke.shiva.sbs_iam.modules.iam.domain.entity.security;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.enums.NotificationChannel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "otp_record", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt",
                column = @Column),
        @AttributeOverride(name = "updatedAt",
                column = @Column)})
public class OtpRecordEntity extends BaseEntity {

    @NotNull
    @Column(name = "channel", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @NotNull
    @Column(name = "notification_channel", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationChannel notificationChannel;

    @Size(max = 100)
    @NotNull
    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Size(max = 255)
    @NotNull
    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @NotNull
    @Column(name = "expiry_time", nullable = false)
    private OffsetDateTime expiryTime;

    @ColumnDefault("0")
    @Column(name = "verify_attempts")
    private Short verifyAttempts;

    @Size(max = 20)
    @ColumnDefault("'PENDING'")
    @Column(name = "status", length = 20)
    private String status;

    @Size(max = 255)
    @Column(name = "\"to\"", length = 255)
    private String to;
}