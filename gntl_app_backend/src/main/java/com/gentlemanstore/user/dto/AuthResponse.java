package com.gentlemanstore.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;

    private String refreshToken;

    private String email;

    private String firstName;

    private String lastName;

    private String role;

    private Long userId;
}
