package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "countries", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class Country extends BaseEntity {
    @Size(max = 3)
    @NotNull
    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode;

    @Size(max = 5)
    @NotNull
    @Column(name = "phone_code", nullable = false, length = 5)
    private String phoneCode;

    @Size(max = 100)
    @NotNull
    @Column(name = "country_name", nullable = false, length = 100)
    private String countryName;

    @Size(max = 3)
    @NotNull
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Size(max = 100)
    @NotNull
    @Column(name = "currency_name", nullable = false, length = 100)
    private String currencyName;

    @OneToMany(mappedBy = "countryCode")
    private Set<Branch> branches = new LinkedHashSet<>();

    @OneToMany(mappedBy = "countryCode")
    private Set<Organization> organizations = new LinkedHashSet<>();

    @OneToMany(mappedBy = "countryCode")
    private Set<Person> people = new LinkedHashSet<>();

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getPhoneCode() {
        return phoneCode;
    }

    public void setPhoneCode(String phoneCode) {
        this.phoneCode = phoneCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    public Set<Branch> getBranches() {
        return branches;
    }

    public void setBranches(Set<Branch> branches) {
        this.branches = branches;
    }

    public Set<Organization> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(Set<Organization> organizations) {
        this.organizations = organizations;
    }

    public Set<Person> getPeople() {
        return people;
    }

    public void setPeople(Set<Person> people) {
        this.people = people;
    }

}