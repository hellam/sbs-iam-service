package ke.shiva.microservice_template.modules.payment.dto.users.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ke.shiva.microservice_template.modules.payment.entity.User;
import ke.shiva.shivacorestarter.validation.Unique;

public class CreateUsersRequest {
    @NotBlank
    private String fullName;
    @NotBlank
    @Size(min = 3, max = 50)
    @Unique(entity = User.class,field = "username", message = "{common.field.exists}")
    private String username;
    @NotBlank
    @Email
    @Unique(entity = User.class,field = "email", message = "{common.field.exists}")
    private String email;

    @Unique(entity = User.class, field = "idNumber", message = "{common.field.exists}")
    private String idNumber;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }
}
