package com.gentlemanstore.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {
    private Long id;
    private String street;
    private String apartment;
    private String city;
    private String postalCode;
    private String country;
    private boolean isDefault;
}