package com.example.booking;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CreateBookingTest extends ApiTestBase {

    @Test
    void createsBooking() throws Exception {
        postBooking(oslo.getId(), "ana@example.com", tomorrow(10, 0), tomorrow(11, 30), 5)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/bookings/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.roomId").value(oslo.getId()))
                .andExpect(jsonPath("$.roomName").value("Oslo"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.start").value("2026-03-03T10:00:00"));
    }

    @Test
    void overlappingBookingIs409() throws Exception {
        existing(oslo, "bob@example.com", tomorrow(10, 0), tomorrow(11, 0));

        postBooking(oslo.getId(), "ana@example.com", tomorrow(10, 30), tomorrow(11, 30), 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BOOKING_CONFLICT"));
    }

    @Test
    void tooManyAttendeesIs400() throws Exception {
        postBooking(lisbon.getId(), "ana@example.com", tomorrow(10, 0), tomorrow(11, 0), 5)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CAPACITY_EXCEEDED"));
    }

    @Test
    void endBeforeStartIs400() throws Exception {
        postBooking(oslo.getId(), "ana@example.com", tomorrow(11, 0), tomorrow(10, 0), 2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void missingTitleIs400() throws Exception {
        String json = """
                {"roomId": %d, "organizerEmail": "ana@example.com",
                 "start": "2026-03-03T10:00:00", "end": "2026-03-03T11:00:00", "attendees": 2}
                """.formatted(oslo.getId());
        mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownRoomIs404() throws Exception {
        postBooking(999_999L, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0), 2)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"));
    }
}
