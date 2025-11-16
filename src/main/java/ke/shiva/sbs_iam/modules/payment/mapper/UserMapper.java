package ke.shiva.sbs_iam.modules.payment.mapper;

import ke.shiva.sbs_iam.modules.payment.dto.users.response.UserDto;
import ke.shiva.sbs_iam.modules.payment.entity.User;
import ke.shiva.shivacorestarter.util.EncryptionUtil;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private final EncryptionUtil encryptionUtil;

    public UserMapper(final EncryptionUtil encryptionUtil) {
        this.encryptionUtil = encryptionUtil;
    }

    public UserDto toDto(User user) {
        if (user == null) return null;

        return new UserDto(
                encryptionUtil.encrypt(user.getId().toString()),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public User toEntity(UserDto dto) {
        if (dto == null) return null;

        User user = new User();
        user.setId(Long.parseLong(encryptionUtil.decrypt(dto.getId()))); // careful: decrypt
        user.setFullName(dto.getFullName());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setCreatedAt(dto.getCreatedAt());
        user.setUpdatedAt(dto.getUpdatedAt());
        return user;
    }
}
