package com.gentlemanstore.loyalty.service;

import com.gentlemanstore.common.exception.BadRequestException;
import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.loyalty.dto.LoyaltyAccountDTO;
import com.gentlemanstore.loyalty.dto.LoyaltyTransactionDTO;
import com.gentlemanstore.loyalty.mapper.LoyaltyMapper;
import com.gentlemanstore.loyalty.model.LoyaltyAccount;
import com.gentlemanstore.loyalty.model.LoyaltyTransaction;
import com.gentlemanstore.loyalty.reporitory.LoyaltyAccountRepository;
import com.gentlemanstore.loyalty.reporitory.LoyaltyTierRepository;
import com.gentlemanstore.loyalty.reporitory.LoyaltyTransactionRepository;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;
    private final UserRepository userRepository;
    private final LoyaltyMapper mapper;

    @Transactional(readOnly = true)
    public LoyaltyAccountDTO getAccount(Long id){
        LoyaltyAccount loyaltyAccount = loyaltyAccountRepository.findByUserIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found"));

        return mapper.toDTO(loyaltyAccount);
    }

    @Transactional(readOnly = true)
    public Page<LoyaltyTransactionDTO> getTransactions(Long userId, Pageable pageable) {
        LoyaltyAccount account = loyaltyAccountRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found"));

        return loyaltyTransactionRepository.findAllByLoyaltyAccountIdAndDeletedFalse(account.getId(), pageable)
                .map(mapper::toTransactionDTO);
    }

    @Transactional()
    public LoyaltyAccountDTO createAccount(Long userId){
        com.gentlemanstore.loyalty.model.LoyaltyTier tier = loyaltyTierRepository
                .findTopByMinPointsLessThanEqualOrderByMinPointsDesc(0)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty tier not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LoyaltyAccount account = LoyaltyAccount.builder()
                .points(0)
                .user(user)
                .loyaltyTier(tier)
                .build();

        loyaltyAccountRepository.save(account);
        return mapper.toDTO(account);
    }

    @Transactional()
    public LoyaltyAccountDTO addPoints(Long userId, Integer points, String description) {
        LoyaltyAccount account = loyaltyAccountRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found"));

        int newBalance = account.getPoints() + points;
        if (newBalance < 0) {
            throw new BadRequestException("Resulting points balance cannot be negative");
        }
        account.setPoints(newBalance);

        loyaltyTierRepository.findTopByMinPointsLessThanEqualOrderByMinPointsDesc(account.getPoints())
                .ifPresent(account::setLoyaltyTier);

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .points(points)
                .description(description)
                .loyaltyAccount(account)
                .build();

        loyaltyTransactionRepository.save(transaction);
        loyaltyAccountRepository.save(account);
        return mapper.toDTO(account);

    }
}
