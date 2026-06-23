package com.gentlemanstore.user.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.security.RefreshTokenService;
import com.gentlemanstore.user.dto.UpdateUserRequest;
import com.gentlemanstore.user.dto.UserDTO;
import com.gentlemanstore.user.mapper.UserMapper;
import com.gentlemanstore.user.model.Role;
import com.gentlemanstore.user.model.RoleName;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.RoleRepository;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService{

    private final UserRepository repo;
    private final UserMapper mapper;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public UserDTO getProfile(Long id, User currentUser){
        if (!isStaff(currentUser) && !id.equals(currentUser.getId())) {
            throw new ResourceNotFoundException("User not found");
        }

        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapper.toDTO(user);
    }

    private boolean isStaff(User user) {
        return user.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_MANAGER"));
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable, Boolean deleted) {
        Page<User> users;
        if (deleted == null) {
            users = repo.findAll(pageable);
        } else {
            users = deleted ? repo.findAllByDeletedTrue(pageable) : repo.findAllByDeletedFalse(pageable);
        }
        return users.map(user -> {
            UserDTO dto = mapper.toDTO(user);
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                String role = user.getRoles().iterator().next().getName().name().replace("ROLE_", "");
                dto.setRole(role);
            }
            dto.setDeleted(user.isDeleted());
            return dto;
        });
    }

    @Transactional()
    public UserDTO updateProfile(Long id, UpdateUserRequest request, User currentUser){
        if (!isStaff(currentUser) && !id.equals(currentUser.getId())) {
            throw new ResourceNotFoundException("User not found");
        }

        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());

        repo.save(user);

        return mapper.toDTO(user);
    }

    @Transactional()
    public void deleteAccount(Long id){
        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setDeleted(true);

        repo.save(user);
        refreshTokenService.revokeAllForUser(id);
    }

    @Transactional
    public UserDTO changeRole(Long id, RoleName roleName) {
        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        user.setRoles(new HashSet<>(Set.of(role)));
        repo.save(user);
        refreshTokenService.revokeAllForUser(id);

        UserDTO dto = mapper.toDTO(user);
        dto.setRole(roleName.name().replace("ROLE_", ""));
        return dto;
    }

    @Transactional
    public UserDTO reactivateUser(Long id) {
        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setDeleted(false);
        repo.save(user);
        UserDTO dto = mapper.toDTO(user);
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            String role = user.getRoles().iterator().next().getName().name().replace("ROLE_", "");
            dto.setRole(role);
        }
        return dto;
    }


}
