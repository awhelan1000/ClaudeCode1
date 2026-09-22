package com.marlowefinch.ops;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TicketController {

    private final DashboardRepository repository;
    private final Clock clock;

    public TicketController(DashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @GetMapping("/api/tickets/by-category")
    public List<TicketCategoryCount> byCategory(@RequestParam(required = false) String from,
                                                @RequestParam(required = false) String to) {
        List<String> errors = new ArrayList<>();
        DateRange range = RequestValidation.dateRange(from, to, clock, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return repository.ticketsByCategory(range);
    }
}
