package com.gentlemanstore.cart.repository;

import com.gentlemanstore.cart.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByIdAndDeletedFalse(Long id);
    List<CartItem> findAllByCartIdAndDeletedFalse(Long cartId);
}
