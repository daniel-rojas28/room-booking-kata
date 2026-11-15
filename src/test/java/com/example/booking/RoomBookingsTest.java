package com.example.booking;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class RoomBookingsTest extends ApiTestBase {

    @Test
    void confirmedBookingsInWindowSortedByStart() throws Exception {
        existing(oslo, "ana@example.com", tomorrow(14, 0), tomorrow(15, 0));
        existing(oslo, "bob@example.com", tomorrow(9, 0), tomorrow(10, 0));
        existing(oslo, "eve@example.com", tomorrow(19, 0), tomorrow(20, 0));   // outside the window
        existing(quito, "ana@example.com", tomorrow(9, 0), tomorrow(10, 0));   // other room

        mvc.perform(get("/rooms/{id}/bookings", oslo.getId())
                        .param("from", "2026-03-03T08:00:00")
                        .param("to", "2026-03-03T18:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].organizerEmail", contains("bob@example.com", "ana@example.com")));
    }
}
