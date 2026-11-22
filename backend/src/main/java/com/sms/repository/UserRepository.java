package com.sms.repository;

import com.sms.model.AppUser;
import com.sms.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    long countByRoleAndActive(Role role, boolean active);
    List<AppUser> findByRole(Role role);
    List<AppUser> findByRoleOrderByFullNameAsc(Role role);
}
