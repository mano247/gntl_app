package com.gentlemanstore.user.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
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

    @Transactional(readOnly = true)
    public UserDTO getProfile(Long id){
        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapper.toDTO(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return repo.findAllByDeletedFalse(pageable)
                .map(mapper::toDTO);
    }

    @Transactional()
    public UserDTO updateProfile(Long id, UpdateUserRequest request){
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
    }

    @Transactional()
    public UserDTO changeRole(Long id, String roleName) {
        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role role = roleRepository.findByName(RoleName.valueOf(roleName))
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        user.setRoles(new HashSet<>(Set.of(role)));
        repo.save(user);

        return mapper.toDTO(user);
    }

}
