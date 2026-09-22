package com.marlowefinch.ops;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Shared parameter validation for the query endpoints (/api/kpis,
 * /api/deliveries/on-time, /api/deliveries/late, /api/tickets/by-category).
 *
 * Each method appends any problems it finds to the caller's {@code errors} list
 * instead of throwing immediately, so a single request can report several
 * problems at once (AC-4). Callers check {@code errors} after gathering every
 * parameter and throw a {@link ValidationException} if it is non-empty.
 */
final class RequestValidation {

    static final int MAX_SPAN_DAYS = 366;
    static final int MIN_LIMIT = 1;
    static final int MAX_LIMIT = 500;

    private RequestValidation() {
    }

    /**
     * Parses and validates {@code from}/{@code to}, applying the "last 30 days
     * ending today" defaults for whichever is missing. Returns {@code null} (and
     * appends to {@code errors}) if the parameters are invalid; callers must not
     * use the return value unless {@code errors} is still empty afterwards.
     */
    static DateRange dateRange(String from, String to, Clock clock, List<String> errors) {
        LocalDate start = parseDate(from, "from", errors);
        LocalDate end = parseDate(to, "to", errors);
        boolean fromPresent = from != null && !from.isBlank();
        boolean toPresent = to != null && !to.isBlank();
        if ((fromPresent && start == null) || (toPresent && end == null)) {
            return null;
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate resolvedEnd = end == null ? today : end;
        LocalDate resolvedStart = start == null ? today.minusDays(DateRange.DEFAULT_DAYS) : start;

        if (resolvedStart.isAfter(resolvedEnd)) {
            errors.add("from must be on or before to");
            return null;
        }
        if (ChronoUnit.DAYS.between(resolvedStart, resolvedEnd) > MAX_SPAN_DAYS) {
            errors.add("range must not span more than " + MAX_SPAN_DAYS + " days");
            return null;
        }
        return new DateRange(resolvedStart, resolvedEnd);
    }

    private static LocalDate parseDate(String value, String paramName, List<String> errors) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            errors.add(paramName + " must be an ISO date (YYYY-MM-DD)");
            return null;
        }
    }

    /**
     * Parses and validates {@code limit}, applying {@code defaultValue} if the
     * parameter is missing. Returns {@code null} (and appends to {@code errors})
     * if it is present but not an integer between 1 and 500.
     */
    static Integer limit(String limit, int defaultValue, List<String> errors) {
        if (limit == null || limit.isBlank()) {
            return defaultValue;
        }
        int value;
        try {
            value = Integer.parseInt(limit.trim());
        } catch (NumberFormatException e) {
            errors.add("limit must be an integer between " + MIN_LIMIT + " and " + MAX_LIMIT);
            return null;
        }
        if (value < MIN_LIMIT || value > MAX_LIMIT) {
            errors.add("limit must be an integer between " + MIN_LIMIT + " and " + MAX_LIMIT);
            return null;
        }
        return value;
    }
}
