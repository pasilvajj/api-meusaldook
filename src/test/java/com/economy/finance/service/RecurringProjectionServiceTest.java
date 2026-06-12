package com.economy.finance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecurringProjectionServiceTest {

    @Test
    void projectedIdUsesSourceTransactionWhenRecurringIdIsNull() {
        long id = RecurringProjectionService.projectedId(null, 42L, 3);
        assertEquals(-420003L, id);
        assertTrue(id < 0);
    }

    @Test
    void projectedIdPrefersRecurringId() {
        assertEquals(-500007L, RecurringProjectionService.projectedId(50L, 42L, 7));
    }
}
