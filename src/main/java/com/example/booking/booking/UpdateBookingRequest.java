package com.example.booking.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PATCH body: every field is optional, null means "not sent". */
public record UpdateBookingRequest(
        @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String title,
        @Min(1) Integer attendees) {
}
