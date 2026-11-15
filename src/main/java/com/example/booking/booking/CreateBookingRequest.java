package com.example.booking.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateBookingRequest(
        @NotNull Long roomId,
        @NotBlank @Email String organizerEmail,
        @NotBlank @Size(max = 100) String title,
        @NotNull LocalDateTime start,
        @NotNull LocalDateTime end,
        @NotNull @Min(1) Integer attendees) {
}
