package com.example.booking.booking;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long roomId,
        String roomName,
        String organizerEmail,
        String title,
        LocalDateTime start,
        LocalDateTime end,
        int attendees,
        BookingStatus status,
        long version) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getRoom().getId(), b.getRoom().getName(), b.getOrganizerEmail(),
                b.getTitle(), b.getStart(), b.getEnd(), b.getAttendees(), b.getStatus(), b.getVersion());
    }

    public String etag() {
        return "\"" + version + "\"";
    }
}
