package com.economy.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_account")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser owner;

    @Column(name = "public_key", nullable = false, length = 64)
    private String publicKey;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 32)
    private AccountType accountType;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "initial_balance_date", nullable = false)
    private LocalDate initialBalanceDate;

    @Column(name = "initial_balance_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal initialBalanceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "saldo_creditor_debtor", nullable = false, length = 16)
    private SaldoCreditorDebtor saldoCreditorDebtor;

    @Enumerated(EnumType.STRING)
    @Column(name = "consider_balance_mode", nullable = false, length = 32)
    private ConsiderBalanceMode considerBalanceMode;

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
