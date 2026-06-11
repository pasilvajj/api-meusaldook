package com.economy.finance.service;

import com.economy.finance.api.dto.AddressPatchRequest;
import com.economy.finance.api.dto.ContactPatchRequest;
import com.economy.finance.api.dto.MeResponse;
import com.economy.finance.api.dto.PersonalInfoPatchRequest;
import com.economy.finance.api.exception.ResourceNotFoundException;
import com.economy.finance.domain.AppUser;
import com.economy.finance.domain.UserAddress;
import com.economy.finance.persistence.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public MeResponse getProfile(Long userId) {
        return toResponse(requireUser(userId));
    }

    @Transactional
    public MeResponse updatePersonal(Long userId, PersonalInfoPatchRequest req) {
        AppUser u = requireUser(userId);
        u.setFullName(req.fullName().trim());
        u.setGender(req.gender());
        u.setCpf(trimToNull(req.cpf()));
        u.setBirthDate(req.birthDate());
        return toResponse(appUserRepository.save(u));
    }

    @Transactional
    public MeResponse updateAddress(Long userId, AddressPatchRequest req) {
        AppUser u = requireUser(userId);
        String street = trimToNull(req.street());
        String number = trimToNull(req.number());
        String complement = trimToNull(req.complement());
        String postalCode = trimToNull(req.postalCode());
        String city = trimToNull(req.city());
        String stateRaw = trimToNull(req.state());
        String state = stateRaw != null ? stateRaw.toUpperCase() : null;

        boolean allBlank =
                street == null
                        && number == null
                        && complement == null
                        && postalCode == null
                        && city == null
                        && state == null;

        UserAddress addr = u.getAddress();
        if (allBlank) {
            if (addr != null) {
                u.setAddress(null);
            }
        } else {
            if (addr == null) {
                addr = new UserAddress();
                addr.setUser(u);
                u.setAddress(addr);
            }
            addr.setStreet(street);
            addr.setNumber(number);
            addr.setComplement(complement);
            addr.setPostalCode(postalCode);
            addr.setCity(city);
            addr.setStateCode(state);
        }
        return toResponse(appUserRepository.save(u));
    }

    @Transactional
    public MeResponse updateContact(Long userId, ContactPatchRequest req) {
        AppUser u = requireUser(userId);
        u.setPhone(trimToNull(req.phone()));
        return toResponse(appUserRepository.save(u));
    }

    private AppUser requireUser(Long userId) {
        return appUserRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado"));
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static MeResponse toResponse(AppUser u) {
        UserAddress a = u.getAddress();
        return MeResponse.builder()
                .email(u.getEmail())
                .fullName(u.getFullName())
                .gender(u.getGender())
                .cpf(u.getCpf())
                .birthDate(u.getBirthDate())
                .street(a != null ? a.getStreet() : null)
                .number(a != null ? a.getNumber() : null)
                .complement(a != null ? a.getComplement() : null)
                .postalCode(a != null ? a.getPostalCode() : null)
                .city(a != null ? a.getCity() : null)
                .state(a != null ? a.getStateCode() : null)
                .phone(u.getPhone())
                .build();
    }
}
