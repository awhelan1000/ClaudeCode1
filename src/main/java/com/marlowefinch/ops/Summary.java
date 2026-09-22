package com.marlowefinch.ops;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Stand-up summary for a date range: the four headline KPIs plus the worst-performing
 * carrier and the busiest ticket category. {@code worstCarrier} and
 * {@code busiestTicketCategory} are null when the range is empty.
 */
public record Summary(
        LocalDate from,
        LocalDate to,
        Double onTimeRate,
        long openTickets,
        BigDecimal revenue,
        long orders,
        String worstCarrier,
        String busiestTicketCategory) {
}
