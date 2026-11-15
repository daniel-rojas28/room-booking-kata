package com.example.booking.booking;

import com.example.booking.error.ApiException;
import com.example.booking.room.RoomRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final Clock clock;

    public BookingService(BookingRepository bookings, RoomRepository rooms, Clock clock) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long id) {
        Booking booking = bookings.findById(id).orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking " + id + " not found"));
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> roomBookings(Long roomId, LocalDateTime from, LocalDateTime to) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings.findAll()) {
            if (b.getRoom().getId().equals(roomId)
                    && b.getStatus() == BookingStatus.CONFIRMED
                    && b.getStart().isBefore(to)
                    && b.getEnd().isAfter(from)) {
                result.add(b);
            }
        }
        result.sort((a, b) -> a.getStart().compareTo(b.getStart()));
        List<BookingResponse> responses = new ArrayList<>();
        for (Booking b : result) {
            responses.add(BookingResponse.from(b));
        }
        return responses;
    }
}
