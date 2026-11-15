package com.example.booking.booking;

import java.time.LocalDateTime;

public record CreateBookingRequest(
        Long roomId,
        String organizerEmail,
        String title,
        LocalDateTime start,
        LocalDateTime end,
        Integer attendees) {
}
