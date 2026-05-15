package com.gentlemanstore.user.repository;

import com.gentlemanstore.user.model.Role;
import com.gentlemanstore.user.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
