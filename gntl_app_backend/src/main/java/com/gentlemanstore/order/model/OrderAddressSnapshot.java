package com.gentlemanstore.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAddressSnapshot {

    @Column(name = "shipping_street", nullable = false)
    private String street;

    @Column(name = "shipping_apartment")
    private String apartment;

    @Column(name = "shipping_city", nullable = false)
    private String city;

    @Column(name = "shipping_postal_code", nullable = false)
    private String postalCode;

    @Column(name = "shipping_country", nullable = false)
    private String country;
}