package com.gentlemanstore.user.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.user.dto.AddressDTO;
import com.gentlemanstore.user.mapper.AddressMapper;
import com.gentlemanstore.user.model.Address;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.AddressRepository;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Transactional(readOnly = true)
    public List<AddressDTO> getMyAddresses(Long userId) {
        return addressRepository.findAllByUserIdAndDeletedFalse(userId).stream()
                .map(addressMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressDTO createAddress(Long userId, AddressDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.isDefault()) {
            clearExistingDefault(userId);
        }

        Address address = Address.builder()
                .street(request.getStreet())
                .apartment(request.getApartment())
                .city(request.getCity())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .isDefault(request.isDefault())
                .deleted(false)
                .user(user)
                .build();

        return addressMapper.toDTO(addressRepository.save(address));
    }

    @Transactional
    public AddressDTO updateAddress(Long userId, Long addressId, AddressDTO request) {
        Address address = addressRepository.findByIdAndDeletedFalse(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found");
        }

        if (request.isDefault() && !address.isDefault()) {
            clearExistingDefault(userId);
        }

        address.setStreet(request.getStreet());
        address.setApartment(request.getApartment());
        address.setCity(request.getCity());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setDefault(request.isDefault());

        return addressMapper.toDTO(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndDeletedFalse(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found");
        }

        address.setDeleted(true);
        addressRepository.save(address);
    }

    private void clearExistingDefault(Long userId) {
        List<Address> addresses = addressRepository.findAllByUserIdAndDeletedFalse(userId);
        addresses.forEach(a -> a.setDefault(false));
        addressRepository.saveAll(addresses);
    }
}