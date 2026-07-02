package com.gentlemanstore.user.repository;

import com.gentlemanstore.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndDeletedFalse(String email);
    boolean existsByEmail(String email);
    Page<User> findAllByDeletedTrue(Pageable pageable);
    Page<User> findAllByDeletedFalse(Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.deleted = false AND MONTH(u.createdAt) = MONTH(CURRENT_DATE) AND YEAR(u.createdAt) = YEAR(CURRENT_DATE)")
    Integer countNewUsersThisMonth();

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE u.deleted = false AND r.name = :roleName")
    List<User> findAllActiveByRoleName(@org.springframework.data.repository.query.Param("roleName") com.gentlemanstore.user.model.RoleName roleName);
}
