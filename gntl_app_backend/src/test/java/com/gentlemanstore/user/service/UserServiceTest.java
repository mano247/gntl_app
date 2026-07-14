package com.gentlemanstore.user.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.security.RefreshTokenService;
import com.gentlemanstore.user.dto.UserDTO;
import com.gentlemanstore.user.mapper.UserMapper;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository repo;

    @Mock
    private UserMapper mapper;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    @Test
    void changeRoleReplacesRolesRevokesTokensAndStripsPrefix() {
        User user = new User();
        user.setId(5L);
        Role customerRole = new Role();
        customerRole.setName(RoleName.ROLE_CUSTOMER);
        user.getRoles().add(customerRole);

        Role managerRole = new Role();
        managerRole.setName(RoleName.ROLE_MANAGER);

        when(repo.findById(5L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ROLE_MANAGER)).thenReturn(Optional.of(managerRole));
        when(mapper.toDTO(user)).thenReturn(new UserDTO());

        UserDTO dto = userService.changeRole(5L, RoleName.ROLE_MANAGER);

        assertEquals(1, user.getRoles().size(), "stara rola se zamenjuje, ne dodaje");
        assertTrue(user.getRoles().contains(managerRole));
        assertEquals("MANAGER", dto.getRole(), "DTO nosi rolu bez ROLE_ prefiksa (kao i lista korisnika)");
        verify(repo).save(user);
        verify(refreshTokenService).revokeAllForUser(5L);
    }

    @Test
    void changeRoleUserNotFoundThrows404() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.changeRole(99L, RoleName.ROLE_ADMIN));
        verify(repo, never()).save(any());
    }

    @Test
    void changeRoleUnknownRoleThrows404() {
        User user = new User();
        user.setId(5L);
        when(repo.findById(5L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.changeRole(5L, RoleName.ROLE_ADMIN));
        verify(refreshTokenService, never()).revokeAllForUser(anyLong());
    }
}
