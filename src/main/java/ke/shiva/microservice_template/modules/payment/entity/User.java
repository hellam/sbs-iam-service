package ke.shiva.microservice_template.modules.payment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;

@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Size(max = 200)
    @NotNull
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Size(max = 50)
    @NotNull
    @Column(nullable = false,unique = true, length = 50)
    private String username;

    @Size(max = 100)
    @NotNull
    @Column(nullable = false,unique = true, length = 100)
    private String email;

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
}