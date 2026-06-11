package com.economy.finance.api.dto;

import com.economy.finance.domain.Category;
import com.economy.finance.domain.MoneyKind;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CategoryResponse {
    Long id;
    String name;
    MoneyKind kind;
    Long parentId;
    Instant createdAt;

    public static CategoryResponse from(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .kind(c.getKind())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .createdAt(c.getCreatedAt())
                .build();
    }
}
