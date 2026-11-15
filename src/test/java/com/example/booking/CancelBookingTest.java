package com.example.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.booking.booking.Booking;
import com.example.booking.booking.BookingStatus;
import org.junit.jupiter.api.Test;

class CancelBookingTest extends ApiTestBase {

    @Test
    void organizerCancels() throws Exception {
        Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));

        mvc.perform(post("/bookings/{id}/cancel", b.getId()).header("X-User-Email", "ana@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        org.assertj.core.api.Assertions.assertThat(bookings.findById(b.getId()))
                .get().extracting(Booking::getStatus).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void someoneElseIs403() throws Exception {
        Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));

        mvc.perform(post("/bookings/{id}/cancel", b.getId()).header("X-User-Email", "bob@example.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NOT_ORGANIZER"));
    }

    @Test
    void cancellingTwiceIs409() throws Exception {
        Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
        mvc.perform(post("/bookings/{id}/cancel", b.getId()).header("X-User-Email", "ana@example.com"));

        mvc.perform(post("/bookings/{id}/cancel", b.getId()).header("X-User-Email", "ana@example.com"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_CANCELLED"));
    }

    @Test
    void unknownBookingIs404() throws Exception {
        mvc.perform(post("/bookings/{id}/cancel", 999_999).header("X-User-Email", "ana@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("BOOKING_NOT_FOUND"));
    }
}
