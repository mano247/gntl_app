package com.gentlemanstore.user.service;

import com.gentlemanstore.common.exception.EmailAlreadyExistsException;
import com.gentlemanstore.common.util.EmailService;
import com.gentlemanstore.security.JwtService;
import com.gentlemanstore.user.dto.AuthResponse;
import com.gentlemanstore.user.dto.RegisterRequest;
import com.gentlemanstore.user.model.Role;
import com.gentlemanstore.user.model.RoleName;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.RoleRepository;
import com.gentlemanstore.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private com.gentlemanstore.security.RefreshTokenService refreshTokenService;

    @Mock
    private com.gentlemanstore.loyalty.reporitory.LoyaltyAccountRepository loyaltyAccountRepository;

    @Mock
    private com.gentlemanstore.loyalty.reporitory.LoyaltyTierRepository loyaltyTierRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("jovann@gmail.com");
        request.setPassword("jovanjovannn");
        request.setFirstName("Test");
        request.setLastName("User");

        when(userRepository.existsByEmail("jovann@gmail.com")).thenReturn(true);

        // when + then
        assertThrows(EmailAlreadyExistsException.class, () -> {
            authService.register(request);
        });
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("novi@gmail.com");
        request.setPassword("password123");
        request.setFirstName("Novi");
        request.setLastName("Korisnik");

        Role role = new Role();
        role.setName(RoleName.ROLE_CUSTOMER);

        User savedUser = User.builder()
                .email("novi@gmail.com")
                .firstName("Novi")
                .lastName("Korisnik")
                .roles(Set.of(role))
                .build();

        when(userRepository.existsByEmail("novi@gmail.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any())).thenReturn("jwtToken");
        when(refreshTokenService.issueRefreshToken(any())).thenReturn("refreshToken");
        when(loyaltyTierRepository.findTopByMinPointsLessThanEqualOrderByMinPointsDesc(0))
                .thenReturn(Optional.of(com.gentlemanstore.loyalty.model.LoyaltyTier.builder()
                        .id(1L)
                        .name("Gentleman")
                        .minPoints(0)
                        .discountPercentage(java.math.BigDecimal.ZERO)
                        .build()));

        // when
        AuthResponse response = authService.register(request);

        // then
        assertThat(response.getToken()).isEqualTo("jwtToken");
        assertThat(response.getEmail()).isEqualTo("novi@gmail.com");
    }
}
