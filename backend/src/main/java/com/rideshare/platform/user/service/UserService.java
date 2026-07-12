package com.rideshare.platform.user.service;

import com.rideshare.platform.common.AgeValidator;
import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.user.dto.UpdateProfileRequest;
import com.rideshare.platform.user.dto.UserProfileResponse;
import com.rideshare.platform.user.entity.User;
import com.rideshare.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getByPublicId(String publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> ApiException.notFound("USER_001", "User not found."));
    }

    public UserProfileResponse getProfile(String publicId) {
        return toResponse(getByPublicId(publicId));
    }

    @Transactional
    public UserProfileResponse updateProfile(String publicId, UpdateProfileRequest request) {
        User user = getByPublicId(publicId);

        if (request.name() != null) user.setName(request.name());
        if (request.profilePhotoUrl() != null) user.setProfilePhotoUrl(request.profilePhotoUrl());
        if (request.gender() != null) user.setGender(request.gender());
        if (request.dob() != null) {
            AgeValidator.requireAtLeast18(request.dob());
            user.setDob(request.dob());
        }
        if (request.preferredLanguage() != null) user.setPreferredLanguage(request.preferredLanguage());
        if (request.homeLat() != null) user.setHomeLat(request.homeLat());
        if (request.homeLng() != null) user.setHomeLng(request.homeLng());
        if (request.officeLat() != null) user.setOfficeLat(request.officeLat());
        if (request.officeLng() != null) user.setOfficeLng(request.officeLng());

        return toResponse(userRepository.save(user));
    }

    /** Grants ADMIN. Called only from AdminUserController, itself already ADMIN-gated by SecurityConfig. */
    @Transactional
    public UserProfileResponse promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_001", "User not found."));
        user.setRoleAdmin(true);
        return toResponse(userRepository.save(user));
    }

    private UserProfileResponse toResponse(User u) {
        return new UserProfileResponse(
                u.getPublicId(), u.getName(), u.getEmail(), u.getMobile(), u.getGender(), u.getDob(),
                u.getProfilePhotoUrl(), u.getPreferredLanguage(), u.getHomeLat(), u.getHomeLng(),
                u.getOfficeLat(), u.getOfficeLng(), u.isRolePassenger(), u.isRoleDriver(), u.isRoleAdmin(),
                u.getAverageRating());
    }
}
