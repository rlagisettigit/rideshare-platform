package com.rideshare.platform.user.repository;

import com.rideshare.platform.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMobile(String mobile);
    Optional<User> findByPublicId(String publicId);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
    boolean existsByMobileAndPublicIdNot(String mobile, String publicId);
    boolean existsByRoleAdminTrue();
}
