package com.rideshare.platform.user.entity;

import com.rideshare.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FR-001 User Registration, FR-003 Profile Management.
 * A single user row backs Passenger / Driver / Both roles (role_passenger, role_driver flags).
 */
@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private String publicId = UUID.randomUUID().toString();

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    // Nullable: a user provisioned via Google/Apple sign-in has no phone number from the ID
    // token (see AuthService.provisionSocialUser). Still unique - InnoDB allows multiple NULLs
    // under a unique index, just not duplicate non-NULL values.
    @Column(unique = true, length = 20)
    private String mobile;

    // Set true once the post-registration OTP (delivered via MSG91, see OtpService /
    // AuthService.verifyMobile) is confirmed. False for users with no mobile (social signup).
    @Column(name = "mobile_verified", nullable = false)
    private boolean mobileVerified = false;

    @Column(name = "password_hash")
    private String passwordHash;

    private String gender;

    private LocalDate dob;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "preferred_language")
    private String preferredLanguage = "en";

    @Column(name = "home_lat")
    private Double homeLat;
    @Column(name = "home_lng")
    private Double homeLng;
    @Column(name = "office_lat")
    private Double officeLat;
    @Column(name = "office_lng")
    private Double officeLng;

    @Column(name = "role_passenger", nullable = false)
    private boolean rolePassenger = true;

    @Column(name = "role_driver", nullable = false)
    private boolean roleDriver = false;

    @Column(name = "role_admin", nullable = false)
    private boolean roleAdmin = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_provider")
    private IdentityProvider identityProvider = IdentityProvider.LOCAL;

    @Column(name = "average_rating")
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmergencyContact> emergencyContacts = new ArrayList<>();
}
