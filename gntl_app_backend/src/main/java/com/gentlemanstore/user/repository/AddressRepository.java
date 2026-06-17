package com.gentlemanstore.user.repository;

import com.gentlemanstore.user.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findAllByUserIdAndDeletedFalse(Long userId);
    Optional<Address> findByIdAndDeletedFalse(Long id);
}