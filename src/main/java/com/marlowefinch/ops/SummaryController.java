package com.marlowefinch.ops;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SummaryController {

    private final DashboardRepository repository;
    private final Clock clock;

    public SummaryController(DashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @GetMapping("/api/summary")
    public Summary summary(@RequestParam(required = false) String from,
                            @RequestParam(required = false) String to) {
        List<String> errors = new ArrayList<>();
        DateRange range = RequestValidation.dateRange(from, to, clock, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Kpis kpis = repository.kpis(range);

        String worstCarrier = repository.onTimeByCarrier(range).stream()
                .filter(c -> c.delivered() > 0)
                .min(Comparator.comparing(CarrierOnTime::rate))
                .map(CarrierOnTime::carrier)
                .orElse(null);

        String busiestTicketCategory = repository.ticketsByCategory(range).stream()
                .max(Comparator.comparing(TicketCategoryCount::total))
                .map(TicketCategoryCount::category)
                .orElse(null);

        return new Summary(range.from(), range.to(), kpis.onTimeRate(), kpis.openTickets(),
                kpis.revenue(), kpis.orders(), worstCarrier, busiestTicketCategory);
    }
}
