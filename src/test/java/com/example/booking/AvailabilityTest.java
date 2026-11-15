package com.example.booking;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class AvailabilityTest extends ApiTestBase {

    @Test
    void freeRoomsThatFitSmallestFirst() throws Exception {
        existing(oslo, "bob@example.com", tomorrow(10, 0), tomorrow(11, 0));

        mvc.perform(get("/rooms/available")
                        .param("start", "2026-03-03T10:30:00")
                        .param("end", "2026-03-03T11:30:00")
                        .param("attendees", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Lisbon", "Quito")));
    }

    @Test
    void endBeforeStartIs400() throws Exception {
        mvc.perform(get("/rooms/available")
                        .param("start", "2026-03-03T11:00:00")
                        .param("end", "2026-03-03T10:00:00")
                        .param("attendees", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
