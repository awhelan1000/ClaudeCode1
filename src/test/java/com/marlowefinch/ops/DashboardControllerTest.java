package com.marlowefinch.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The JSON API through MockMvc, plus one real HTTP call to observe the error path. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestRestTemplate http;

    @Test
    void healthReportsUpAndTheFixedToday() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.today").value("2026-09-21"));
    }

    @Test
    void kpisDefaultToTheLast30DaysEndingToday() throws Exception {
        mvc.perform(get("/api/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-08-22"))
                .andExpect(jsonPath("$.to").value("2026-09-21"))
                .andExpect(jsonPath("$.onTimeRate").value(0.937))
                .andExpect(jsonPath("$.openTickets").value(114))
                .andExpect(jsonPath("$.revenue").value(360095.5))
                .andExpect(jsonPath("$.orders").value(624));
    }

    @Test
    void kpisAcceptAnExplicitRange() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-07-01").param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-07-01"))
                .andExpect(jsonPath("$.to").value("2026-07-31"))
                .andExpect(jsonPath("$.orders").value(679))
                .andExpect(jsonPath("$.revenue").value(480209.5));
    }

    @Test
    void onTimeReturnsOneRowPerCarrier() throws Exception {
        mvc.perform(get("/api/deliveries/on-time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[1].carrier").value("Kessler Logistics"))
                .andExpect(jsonPath("$[1].delivered").value(265))
                .andExpect(jsonPath("$[1].onTime").value(238))
                .andExpect(jsonPath("$[1].rate").value(0.8981));
    }

    @Test
    void lateReturnsOrderCarrierDatesAndDaysLateAndRespectsTheLimit() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "2026-09-14").param("to", "2026-09-21").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].orderRef").isString())
                .andExpect(jsonPath("$[0].carrier").isString())
                .andExpect(jsonPath("$[0].promisedDate").isString())
                .andExpect(jsonPath("$[0].deliveredDate").isString())
                .andExpect(jsonPath("$[0].daysLate").isNumber());
    }

    /**
     * TODO-232 AC-2: from after to is now a validation error (400), not a silent
     * empty list. Rewrite of the old lateWithFromAfterToReturnsAnEmptyList, which
     * documented the pre-fix behaviour.
     */
    @Test
    void lateWithFromAfterToReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "2026-09-21").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("from must be on or before to"));
    }

    @Test
    void ticketsByCategoryReturnsOpenAndTotalPerCategory() throws Exception {
        mvc.perform(get("/api/tickets/by-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].category").value("Delivery delay"))
                .andExpect(jsonPath("$[0].open").value(41))
                .andExpect(jsonPath("$[0].total").value(90));
    }

    @Test
    void vendorsIncludeDaysUntilContractEndAndTheNoticeWindowFlag() throws Exception {
        mvc.perform(get("/api/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0].name").value("Volta Parts GmbH"))
                .andExpect(jsonPath("$[0].contractEnd").value("2026-10-15"))
                .andExpect(jsonPath("$[0].noticeDays").value(30))
                .andExpect(jsonPath("$[0].daysUntilContractEnd").value(24))
                .andExpect(jsonPath("$[0].inNoticeWindow").value(true))
                .andExpect(jsonPath("$[7].inNoticeWindow").value(false));
    }

    /**
     * TODO-232 AC-1 / AC-4: a malformed from is now caught by RequestValidation and
     * reported as a 400 with an errors list, instead of blowing up as a 500. Rewrite
     * of the old malformedFromCurrentlyProducesA5xx, which documented the pre-fix
     * behaviour. Uses a real HTTP call (rather than MockMvc) to double-check the
     * error response actually reaches the wire, matching the original test's intent.
     */
    @Test
    void malformedFromReturnsBadRequestWithErrorsList() {
        ResponseEntity<String> response = http.getForEntity("/api/kpis?from=next-tuesday", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("\"errors\"").contains("from must be an ISO date (YYYY-MM-DD)");
    }

    /** TODO-232 AC-1: same check via MockMvc, asserting the exact JSON shape. */
    @Test
    void malformedToReturnsBadRequestWithErrorsList() throws Exception {
        mvc.perform(get("/api/kpis").param("to", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("to must be an ISO date (YYYY-MM-DD)"));
    }

    /** TODO-232 AC-2: a range spanning more than 366 days is rejected. */
    @Test
    void rangeSpanningMoreThan366DaysReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2024-01-01").param("to", "2025-01-02"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("range must not span more than 366 days"));
    }

    /** TODO-232 AC-3: limit below the minimum (1) is rejected. */
    @Test
    void limitOfZeroReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("limit must be an integer between 1 and 500"));
    }

    /** TODO-232 AC-3: limit above the maximum (500) is rejected. */
    @Test
    void limitOf501ReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("limit", "501"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("limit must be an integer between 1 and 500"));
    }

    /** TODO-232 AC-4: several problems in one request produce several entries. */
    @Test
    void severalProblemsInOneRequestProduceSeveralErrorEntries() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "not-a-date").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0]").value("from must be an ISO date (YYYY-MM-DD)"))
                .andExpect(jsonPath("$.errors[1]").value("limit must be an integer between 1 and 500"));
    }

    /**
     * TODO-232 AC-4: the validation rules apply to every endpoint that takes
     * from/to, not just /api/kpis.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/api/kpis", "/api/deliveries/on-time", "/api/deliveries/late", "/api/tickets/by-category"})
    void malformedFromReturnsBadRequestOnEveryDateRangeEndpoint(String path) throws Exception {
        mvc.perform(get(path).param("from", "next-tuesday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("from must be an ISO date (YYYY-MM-DD)"));
    }

    /**
     * TODO-232 AC-5 sanity check: a well-formed request still succeeds post-fix on
     * every endpoint that takes from/to/limit.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/api/kpis", "/api/deliveries/on-time", "/api/deliveries/late", "/api/tickets/by-category"})
    void validExplicitRangeStillReturns200(String path) throws Exception {
        mvc.perform(get(path).param("from", "2026-07-01").param("to", "2026-07-31"))
                .andExpect(status().isOk());
    }
}
