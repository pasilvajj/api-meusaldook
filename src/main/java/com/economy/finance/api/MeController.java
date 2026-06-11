package com.economy.finance.api;

import com.economy.finance.api.dto.AddressPatchRequest;
import com.economy.finance.api.dto.ContactPatchRequest;
import com.economy.finance.api.dto.MeResponse;
import com.economy.finance.api.dto.PersonalInfoPatchRequest;
import com.economy.finance.service.CurrentUserService;
import com.economy.finance.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final CurrentUserService currentUserService;
    private final UserProfileService userProfileService;

    @GetMapping
    public MeResponse me() {
        return userProfileService.getProfile(currentUserService.requireUserId());
    }

    @PatchMapping("/personal")
    public MeResponse patchPersonal(@Valid @RequestBody PersonalInfoPatchRequest body) {
        return userProfileService.updatePersonal(currentUserService.requireUserId(), body);
    }

    @PatchMapping("/address")
    public MeResponse patchAddress(@Valid @RequestBody AddressPatchRequest body) {
        return userProfileService.updateAddress(currentUserService.requireUserId(), body);
    }

    @PatchMapping("/contact")
    public MeResponse patchContact(@Valid @RequestBody ContactPatchRequest body) {
        return userProfileService.updateContact(currentUserService.requireUserId(), body);
    }
}
