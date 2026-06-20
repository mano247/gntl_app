package com.gentlemanstore.user.dto;

import com.gentlemanstore.user.model.Address;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDateTime createdAt;
    private List<AddressDTO> addresses;
    private String role;
    private Boolean deleted;
}
