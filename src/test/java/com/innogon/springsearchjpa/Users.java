package com.innogon.springsearchjpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "USERS")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer userId;

    @Column(name = "FirstName")
    private String userFirstName = "John";

    @Column(name = "isAdmin")
    private Boolean isAdmin = true;

    @Column(name = "LastName")
    private String userLastName = "Doe";

    @Column(name = "email")
    private String userEmail = "john.doe@wanahoo.fr";

    @Column(name = "PostalAddress")
    private String userAddress = "1 rue de l'angleterre";

    @Column(name = "NumberOfChildren")
    private Integer userChildrenNumber = 3;

    @Column(name = "Salary")
    private Float userSalary = 3000.0F;

    @Column(name = "AgeInSeconds")
    private Double userAgeInSeconds = 1261440000.0;

    @Column
    private Date createdAt = new Date();

    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column
    private LocalTime updatedTimeAt = LocalTime.now();

    @Column
    private LocalDate updatedDateAt = LocalDate.now();

    @Column
    private Instant updatedInstantAt = Instant.now();

    @Column
    private Duration validityDuration = Duration.ofDays(30);

    @Column(name = "UserType")
    @Enumerated(EnumType.STRING)
    private UserType type = UserType.TEAM_MEMBER;

    @Column
    private UUID uuid = UUID.randomUUID();

    @Column
    private Short userLevel = 5;

    @Column
    private Boolean active = true;

    public Users() {
    }

    public Users(String userFirstName) {
        this.userFirstName = userFirstName;
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserFirstName() { return userFirstName; }
    public void setUserFirstName(String userFirstName) { this.userFirstName = userFirstName; }

    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }

    public String getUserLastName() { return userLastName; }
    public void setUserLastName(String userLastName) { this.userLastName = userLastName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }

    public Integer getUserChildrenNumber() { return userChildrenNumber; }
    public void setUserChildrenNumber(Integer userChildrenNumber) { this.userChildrenNumber = userChildrenNumber; }

    public Float getUserSalary() { return userSalary; }
    public void setUserSalary(Float userSalary) { this.userSalary = userSalary; }

    public Double getUserAgeInSeconds() { return userAgeInSeconds; }
    public void setUserAgeInSeconds(Double userAgeInSeconds) { this.userAgeInSeconds = userAgeInSeconds; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalTime getUpdatedTimeAt() { return updatedTimeAt; }
    public void setUpdatedTimeAt(LocalTime updatedTimeAt) { this.updatedTimeAt = updatedTimeAt; }

    public LocalDate getUpdatedDateAt() { return updatedDateAt; }
    public void setUpdatedDateAt(LocalDate updatedDateAt) { this.updatedDateAt = updatedDateAt; }

    public Instant getUpdatedInstantAt() { return updatedInstantAt; }
    public void setUpdatedInstantAt(Instant updatedInstantAt) { this.updatedInstantAt = updatedInstantAt; }

    public Duration getValidityDuration() { return validityDuration; }
    public void setValidityDuration(Duration validityDuration) { this.validityDuration = validityDuration; }

    public UserType getType() { return type; }
    public void setType(UserType type) { this.type = type; }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public Short getUserLevel() { return userLevel; }
    public void setUserLevel(Short userLevel) { this.userLevel = userLevel; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Users user = new Users();

        public Builder userFirstName(String v) { user.userFirstName = v; return this; }
        public Builder userLastName(String v) { user.userLastName = v; return this; }
        public Builder isAdmin(Boolean v) { user.isAdmin = v; return this; }
        public Builder userChildrenNumber(Integer v) { user.userChildrenNumber = v; return this; }
        public Builder userSalary(Float v) { user.userSalary = v; return this; }
        public Builder userAgeInSeconds(Double v) { user.userAgeInSeconds = v; return this; }
        public Builder createdAt(Date v) { user.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v) { user.updatedAt = v; return this; }
        public Builder updatedTimeAt(LocalTime v) { user.updatedTimeAt = v; return this; }
        public Builder updatedDateAt(LocalDate v) { user.updatedDateAt = v; return this; }
        public Builder updatedInstantAt(Instant v) { user.updatedInstantAt = v; return this; }
        public Builder validityDuration(Duration v) { user.validityDuration = v; return this; }
        public Builder type(UserType v) { user.type = v; return this; }
        public Builder uuid(UUID v) { user.uuid = v; return this; }
        public Builder userLevel(Short v) { user.userLevel = v; return this; }
        public Builder active(Boolean v) { user.active = v; return this; }
        public Users build() { return user; }
    }
}
