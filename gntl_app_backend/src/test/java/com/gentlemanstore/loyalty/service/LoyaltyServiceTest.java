package com.gentlemanstore.loyalty.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.loyalty.mapper.LoyaltyMapper;
import com.gentlemanstore.loyalty.model.LoyaltyTier;
import com.gentlemanstore.loyalty.reporitory.LoyaltyAccountRepository;
import com.gentlemanstore.loyalty.reporitory.LoyaltyTierRepository;
import com.gentlemanstore.loyalty.reporitory.LoyaltyTransactionRepository;
import com.gentlemanstore.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoyaltyServiceTest {
    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Mock
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Mock
    private LoyaltyTierRepository loyaltyTierRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoyaltyMapper mapper;

    @InjectMocks
    private LoyaltyService loyaltyService;

    @Test
    void shouldThrowExceptionWhenLoyaltyAccountNotFound() {
        when(loyaltyAccountRepository.findByUserIdAndDeletedFalse(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            loyaltyService.addPoints(1L, 100, "Purchase reward");
        });
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundOnCreateAccount() {
        when(loyaltyTierRepository.findTopByMinPointsLessThanEqualOrderByMinPointsDesc(0))
                .thenReturn(Optional.of(new LoyaltyTier()));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            loyaltyService.createAccount(1L);
        });
    }
}
