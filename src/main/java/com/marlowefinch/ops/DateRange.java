package com.marlowefinch.ops;

import java.time.LocalDate;

/**
 * A closed date range for the query endpoints.
 *
 * Both bounds default to "the last 30 days ending today" when the caller omits
 * them. Parsing and validating the raw {@code from}/{@code to} request
 * parameters (including the ordering and 366-day span checks from TODO-232)
 * happens in {@link RequestValidation#dateRange}, which controllers call before
 * building one of these.
 */
public record DateRange(LocalDate from, LocalDate to) {

    public static final int DEFAULT_DAYS = 30;
}
