package com.example.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.booking.booking.Booking;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class EditBookingTest extends ApiTestBase {

    @Test
    void getReturnsTheVersionAsETag() throws Exception {
        Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));

        mvc.perform(get("/bookings/{id}", b.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""));
    }

    @Test
    void organizerChangesTheTitle() throws Exception {
        Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));

        mvc.perform(patch("/bookings/{id}", b.getId())
                        .header("X-User-Email", "ana@example.com").header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Retro\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.title").value("Retro"))
                .andExpect(jsonPath("$.attendees").value(2));
    }

    @Test
    void staleETagIs412() throws Exception {
        Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));

        mvc.perform(patch("/bookings/{id}", b.getId())
                        .header("X-User-Email", "ana@example.com").header("If-Match", "\"7\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Retro\"}"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.error").value("PRECONDITION_FAILED"));
    }

    @Test
    void missingIfMatchIs400() throws Exception {
        Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));

        mvc.perform(patch("/bookings/{id}", b.getId())
                        .header("X-User-Email", "ana@example.com")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Retro\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
