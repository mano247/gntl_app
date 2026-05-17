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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService{

    private final UserRepository repo;
    private final UserMapper mapper;
    private final RoleRepository roleRepository;

    public UserDTO getProfile(Long id){
        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapper.toDTO(user);
    }

    public List<UserDTO> getAllUsers() {
        return repo.findAllByDeletedFalse()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public UserDTO updateProfile(Long id, UpdateUserRequest request){
        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());

        repo.save(user);

        return mapper.toDTO(user);
    }

    public void deleteAccount(Long id){
        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setDeleted(true);

        repo.save(user);
    }

    public UserDTO changeRole(Long id, String roleName) {
        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role role = roleRepository.findByName(RoleName.valueOf(roleName))
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        user.setRoles(Set.of(role));
        repo.save(user);

        return mapper.toDTO(user);
    }

}
