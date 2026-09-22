package com.marlowefinch.ops;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeliveryController {

    static final int DEFAULT_LIMIT = 20;

    private final DashboardRepository repository;
    private final Clock clock;

    public DeliveryController(DashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @GetMapping("/api/deliveries/on-time")
    public List<CarrierOnTime> onTime(@RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to) {
        List<String> errors = new ArrayList<>();
        DateRange range = RequestValidation.dateRange(from, to, clock, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return repository.onTimeByCarrier(range);
    }

    @GetMapping("/api/deliveries/late")
    public List<LateDelivery> late(@RequestParam(required = false) String from,
                                   @RequestParam(required = false) String to,
                                   @RequestParam(required = false) String limit) {
        List<String> errors = new ArrayList<>();
        DateRange range = RequestValidation.dateRange(from, to, clock, errors);
        Integer resolvedLimit = RequestValidation.limit(limit, DEFAULT_LIMIT, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return repository.lateDeliveries(range, resolvedLimit);
    }
}
