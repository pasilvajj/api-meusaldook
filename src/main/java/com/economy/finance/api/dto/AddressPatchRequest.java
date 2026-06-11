package com.economy.finance.api.dto;

import jakarta.validation.constraints.Size;

public record AddressPatchRequest(
        @Size(max = 200) String street,
        @Size(max = 32) String number,
        @Size(max = 120) String complement,
        @Size(max = 16) String postalCode,
        @Size(max = 120) String city,
        @Size(max = 2) String state) {}
