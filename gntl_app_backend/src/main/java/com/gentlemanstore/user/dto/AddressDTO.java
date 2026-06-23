package com.gentlemanstore.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {
    private Long id;

    @NotBlank(message = "Street is required")
    @Size(max = 255, message = "Street must be at most 255 characters")
    private String street;

    @Size(max = 255, message = "Apartment must be at most 255 characters")
    private String apartment;

    @NotBlank(message = "City is required")
    @Size(max = 255, message = "City must be at most 255 characters")
    private String city;

    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code must be at most 20 characters")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(max = 255, message = "Country must be at most 255 characters")
    private String country;

    private boolean isDefault;
}