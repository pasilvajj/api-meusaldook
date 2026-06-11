package com.economy.finance.api.dto;

import jakarta.validation.constraints.Size;

public record ContactPatchRequest(@Size(max = 40) String phone) {}
