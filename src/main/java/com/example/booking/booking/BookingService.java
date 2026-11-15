package com.example.booking.booking;

import com.example.booking.error.ApiException;
import com.example.booking.room.Room;
import com.example.booking.room.RoomRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    static final Duration MAX_DURATION = Duration.ofHours(4);

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
        return BookingResponse.from(find(id));
    }

    @Transactional
    public BookingResponse create(CreateBookingRequest req) {
        validateWindow(req.start(), req.end());
        if (req.start().isBefore(LocalDateTime.now(clock))) {
            throw validation("start must not be in the past");
        }
        if (!onQuarterHour(req.start()) || !onQuarterHour(req.end())) {
            throw validation("start and end must be on a 15-minute boundary");
        }
        if (Duration.between(req.start(), req.end()).compareTo(MAX_DURATION) > 0) {
            throw validation("a booking can last at most 4 hours");
        }
        // lock the room row so two requests for the same room can't both pass the overlap check
        Room room = rooms.findByIdForUpdate(req.roomId())
                .orElseThrow(() -> roomNotFound(req.roomId()));
        if (!room.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "ROOM_INACTIVE", "Room " + room.getId() + " is not active");
        }
        if (req.attendees() > room.getCapacity()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CAPACITY_EXCEEDED",
                    "Room " + room.getName() + " fits " + room.getCapacity() + " people");
        }
        if (bookings.existsOverlap(room.getId(), req.start(), req.end())) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_CONFLICT", "Room is already booked in that time");
        }
        Booking saved = bookings.save(new Booking(room, req.organizerEmail().toLowerCase(Locale.ROOT), req.title(),
                req.start(), req.end(), req.attendees()));
        return BookingResponse.from(saved);
    }

    @Transactional
    public BookingResponse cancel(Long id, String userEmail) {
        Booking booking = find(id);
        if (!booking.getOrganizerEmail().equalsIgnoreCase(userEmail)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_ORGANIZER", "Only the organizer can cancel");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_CANCELLED", "Booking " + id + " is already cancelled");
        }
        if (!booking.getStart().isAfter(LocalDateTime.now(clock))) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_STARTED", "Booking " + id + " has already started");
        }
        booking.cancel();
        bookings.flush();
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> roomBookings(Long roomId, LocalDateTime from, LocalDateTime to) {
        if (!rooms.existsById(roomId)) {
            throw roomNotFound(roomId);
        }
        return bookings.findConfirmedInWindow(roomId, from, to).stream().map(BookingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> byOrganizer(String organizer, int page, int size) {
        if (page < 0) {
            throw validation("page must be 0 or more");
        }
        if (size < 1 || size > 100) {
            throw validation("size must be between 1 and 100");
        }
        return bookings.findByOrganizerEmail(organizer.toLowerCase(Locale.ROOT),
                        PageRequest.of(page, size, Sort.by("start", "id")))
                .map(BookingResponse::from);
    }

    @Transactional
    public BookingResponse update(Long id, String userEmail, String ifMatch, UpdateBookingRequest req) {
        Booking booking = find(id);
        if (!booking.getOrganizerEmail().equalsIgnoreCase(userEmail)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_ORGANIZER", "Only the organizer can change a booking");
        }
        if (!BookingResponse.from(booking).etag().equals(ifMatch)) {
            throw new ApiException(HttpStatus.PRECONDITION_FAILED, "PRECONDITION_FAILED",
                    "Booking " + id + " was changed by someone else; GET it again");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_CANCELLED", "Booking " + id + " is cancelled");
        }
        if (!booking.getStart().isAfter(LocalDateTime.now(clock))) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_STARTED", "Booking " + id + " has already started");
        }
        if (req.attendees() != null && req.attendees() > booking.getRoom().getCapacity()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CAPACITY_EXCEEDED",
                    "Room " + booking.getRoom().getName() + " fits " + booking.getRoom().getCapacity() + " people");
        }
        booking.update(req.title(), req.attendees());
        bookings.flush(); // @Version only goes up on flush; without this the response shows the old ETag
        return BookingResponse.from(booking);
    }

    static void validateWindow(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw validation("start must be before end");
        }
    }

    private Booking find(Long id) {
        return bookings.findById(id).orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking " + id + " not found"));
    }

    private static boolean onQuarterHour(LocalDateTime t) {
        return t.getMinute() % 15 == 0 && t.getSecond() == 0 && t.getNano() == 0;
    }

    static ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    static ApiException roomNotFound(Long id) {
        return new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Room " + id + " not found");
    }
}
