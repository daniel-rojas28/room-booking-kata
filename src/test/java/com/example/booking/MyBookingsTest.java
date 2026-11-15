package com.example.booking;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class MyBookingsTest extends ApiTestBase {

    @Test
    void listsTheOrganizersBookingsSortedByStartWithATotal() throws Exception {
        existing(oslo, "ana@example.com", tomorrow(14, 0), tomorrow(15, 0));
        existing(quito, "ana@example.com", tomorrow(9, 0), tomorrow(10, 0));
        existing(lisbon, "bob@example.com", tomorrow(8, 0), tomorrow(9, 0));

        mvc.perform(get("/bookings").param("organizer", "ana@example.com"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].roomName").value("Quito"))
                .andExpect(jsonPath("$[1].roomName").value("Oslo"));
    }

    @Test
    void pages() throws Exception {
        for (int h = 9; h < 14; h++) {
            existing(oslo, "ana@example.com", tomorrow(h, 0), tomorrow(h, 30));
        }

        mvc.perform(get("/bookings").param("organizer", "ana@example.com").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].start").value("2026-03-03T11:00:00"))
                .andExpect(jsonPath("$[1].start").value("2026-03-03T12:00:00"));
    }

    @Test
    void missingOrganizerIs400() throws Exception {
        mvc.perform(get("/bookings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
