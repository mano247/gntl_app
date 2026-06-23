package com.gentlemanstore.user.dto;

import com.gentlemanstore.user.model.RoleName;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeRoleRequest {
    @NotNull(message = "Role is required")
    private RoleName role;
}
