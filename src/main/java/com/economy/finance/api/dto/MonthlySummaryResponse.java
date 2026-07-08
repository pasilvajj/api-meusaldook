package com.economy.finance.api.dto;

import com.economy.finance.domain.MoneyKind;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MonthlySummaryResponse {
    int year;
    int month;
    List<KindTotal> byKind;
    /** Totais por categoria só de despesas (gráfico «Despesas por categoria», metas de despesa). */
    List<CategoryTotal> byCategory;
    /** Totais por categoria só de receitas (ex.: metas de receita). */
    List<CategoryTotal> byIncomeCategory;
    /**
     * Pagamentos de fatura na conta (ex.: principal). Excluídos de {@link #byCategory} para não
     * duplicar compras já categorizadas no cartão.
     */
    BigDecimal invoicePaymentTotal;

    @Value
    @Builder
    public static class KindTotal {
        MoneyKind kind;
        BigDecimal total;
    }

    @Value
    @Builder
    public static class CategoryTotal {
        String categoryName;
        BigDecimal total;
    }
}
