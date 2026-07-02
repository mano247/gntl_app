package com.gentlemanstore.user.service;

import com.gentlemanstore.common.exception.EmailAlreadyExistsException;
import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.common.util.EmailService;
import com.gentlemanstore.loyalty.model.LoyaltyAccount;
import com.gentlemanstore.loyalty.model.LoyaltyTier;
import com.gentlemanstore.loyalty.reporitory.LoyaltyAccountRepository;
import com.gentlemanstore.loyalty.reporitory.LoyaltyTierRepository;
import com.gentlemanstore.security.JwtService;
import com.gentlemanstore.security.RefreshTokenService;
import com.gentlemanstore.security.model.RefreshToken;
import com.gentlemanstore.user.dto.AuthResponse;
import com.gentlemanstore.user.dto.LoginRequest;
import com.gentlemanstore.user.dto.RegisterRequest;
import com.gentlemanstore.user.model.Role;
import com.gentlemanstore.user.model.RoleName;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.RoleRepository;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        Role role = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .roles(Set.of(role))
                .enabled(true)
                .build();

        userRepository.save(user);

        LoyaltyTier startingTier = loyaltyTierRepository.findTopByMinPointsLessThanEqualOrderByMinPointsDesc(0)
                .orElseThrow(() -> new ResourceNotFoundException("Default loyalty tier not found"));

        LoyaltyAccount loyaltyAccount = LoyaltyAccount.builder()
                .user(user)
                .points(0)
                .loyaltyTier(startingTier)
                .deleted(false)
                .build();

        loyaltyAccountRepository.save(loyaltyAccount);

        String token = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.issueRefreshToken(user);

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(role.getName().name())
                .userId(user.getId())
                .build();

        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            log.warn("Email sending failed: {}", e.getMessage());
        }

        return response;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.issueRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRoles().iterator().next().getName().name())
                .userId(user.getId())
                .build();
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken consumed = refreshTokenService.consumeRefreshToken(rawRefreshToken);
        User user = consumed.getUser();

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = refreshTokenService.issueRefreshToken(user);

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRoles().iterator().next().getName().name())
                .userId(user.getId())
                .build();
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeRefreshToken(rawRefreshToken);
    }
}
