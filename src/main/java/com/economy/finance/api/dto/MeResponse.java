package com.economy.finance.api.dto;

import com.economy.finance.domain.Gender;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MeResponse {
    String email;
    String fullName;
    Gender gender;
    String cpf;
    LocalDate birthDate;
    String street;
    String number;
    String complement;
    String postalCode;
    String city;
    String state;
    String phone;
}
