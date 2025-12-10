package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.security.auth.message.AuthException;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.UserCategory;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrganizationUserAuthRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordVerifier {

    private final CustomerAuthRepository customerAuthRepo;
    private final EmployeeAuthRepository employeeAuthRepo;
    private final OrganizationUserAuthRepository orgUserAuthRepo;

    public boolean verify(SessionEntity session, String rawPassword) {
        Channel channel = session.getChannel();
        IamUserEntity user = session.getIamUser();

        return switch (channel) {
            case INTERNET_BANKING,MOBILE_BANKING      -> verifyCustomer(user, rawPassword);
            case BACKOFFICE      -> verifyEmployee(user, rawPassword);
            default            -> throw BaseException.channelNotAllowed("Unsupported channel user: " + channel);
        };
    }

    private boolean verifyCustomer(IamUserEntity user, String rawPassword) {
        CustomerAuthEntity auth = customerAuthRepo
                .findByIamUser(user);

        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Customer credentials not found");
        }

        return HashUtil.bcryptVerify(rawPassword, auth.getInternetPasswordHash());
    }

    private boolean verifyEmployee(IamUserEntity user, String rawPassword) {
        EmployeeAuthEntity auth = employeeAuthRepo
                .findByIamUser(user);

        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Employee credentials not found");
        }

        return HashUtil.bcryptVerify(rawPassword, auth.getStaffPasswordHash());
    }
}
