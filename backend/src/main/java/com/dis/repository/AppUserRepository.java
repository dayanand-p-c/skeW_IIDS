package com.dis.repository;

import com.dis.model.AppUser;
import com.dis.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM AppUser u WHERE u.active = true AND u.role IN :roles")
    List<AppUser> findActiveUsersByRoles(@Param("roles") List<UserRole> roles);

    @Query("SELECT u FROM AppUser u WHERE " +
           "(:query IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<AppUser> searchUsers(@Param("query") String query, Pageable pageable);
}
