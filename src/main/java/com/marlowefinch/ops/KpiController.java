package com.marlowefinch.ops;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KpiController {

    private final DashboardRepository repository;
    private final Clock clock;

    public KpiController(DashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @GetMapping("/api/kpis")
    public Kpis kpis(@RequestParam(required = false) String from,
                     @RequestParam(required = false) String to) {
        List<String> errors = new ArrayList<>();
        DateRange range = RequestValidation.dateRange(from, to, clock, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return repository.kpis(range);
    }
}
