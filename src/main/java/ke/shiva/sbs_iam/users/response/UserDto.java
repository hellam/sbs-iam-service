package ke.shiva.sbs_iam.users.response;

import com.fasterxml.jackson.annotation.JsonView;
import ke.shiva.shivacorestarter.dto.Views;

import java.time.LocalDateTime;

public class UserDto {
    @JsonView({Views.Detailed.class,Views.Full.class})
    private String id;
    @JsonView({Views.Detailed.class,Views.Full.class})
    private String fullName;
    @JsonView({Views.Minimal.class,Views.Detailed.class,Views.Full.class})
    private String username;
    @JsonView({Views.Detailed.class,Views.Full.class})
    private String email;
    @JsonView({Views.Detailed.class, Views.Full.class})
    private String idNumber;
    @JsonView({Views.Full.class})
    private LocalDateTime createdAt;
    @JsonView({Views.Full.class})
    private LocalDateTime updatedAt;

    public UserDto(String id, String fullName, String username, String email, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
