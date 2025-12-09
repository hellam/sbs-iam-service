package ke.shiva.sbs_iam.modules.iam.domain.entity.rbac;

import jakarta.persistence.*;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name = "org_user_role", schema = "iam_service")
public class OrgUserRoleEntity {
    @EmbeddedId
    private OrgUserRoleIdEntity id;

    @MapsId("organizationUserId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_user_id", nullable = false)
    private OrganizationUserEntity organizationUser;

    @MapsId("orgRoleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_role_id", nullable = false)
    private OrgRoleEntity orgRole;

    @ColumnDefault("now()")
    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

}