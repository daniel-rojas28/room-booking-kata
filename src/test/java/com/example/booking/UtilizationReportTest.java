package com.example.booking;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class UtilizationReportTest extends ApiTestBase {

    @Test
    void utilizationPerActiveRoomBusiestFirst() throws Exception {
        existing(quito, "bob@example.com", tomorrow(9, 0), tomorrow(12, 0));   // 180 min
        existing(oslo, "ana@example.com", tomorrow(14, 0), tomorrow(15, 0));   // 60 min

        mvc.perform(get("/reports/utilization").param("date", "2026-03-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].roomName", contains("Quito", "Oslo", "Lisbon")))
                .andExpect(jsonPath("$[0].bookedMinutes").value(180))
                .andExpect(jsonPath("$[0].utilizationPercent").value(30.0))
                .andExpect(jsonPath("$[2].bookedMinutes").value(0));
    }
}
