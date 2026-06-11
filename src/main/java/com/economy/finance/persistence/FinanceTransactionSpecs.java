package com.economy.finance.persistence;

import com.economy.finance.domain.FinanceTransaction;
import com.economy.finance.domain.MoneyKind;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class FinanceTransactionSpecs {

    private FinanceTransactionSpecs() {}

    public static Specification<FinanceTransaction> forUser(
            Long userId,
            Instant from,
            Instant to,
            Long categoryId,
            MoneyKind kind,
            String accountPublicKey) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("owner").get("id"), userId));
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("occurredAt"), to));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (kind != null) {
                predicates.add(cb.equal(root.get("kind"), kind));
            }
            if (accountPublicKey != null) {
                predicates.add(cb.equal(root.get("account").get("publicKey"), accountPublicKey));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
