package com.example.booking.room;

import com.example.booking.booking.Booking;
import com.example.booking.booking.BookingRepository;
import com.example.booking.error.ApiException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomRepository rooms;
    private final BookingRepository bookings;

    public RoomService(RoomRepository rooms, BookingRepository bookings) {
        this.rooms = rooms;
        this.bookings = bookings;
    }

    public List<Room> all() {
        return rooms.findAll();
    }

    public Room get(Long id) {
        return rooms.findById(id).orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Room " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Room> available(LocalDateTime start, LocalDateTime end, int attendees) {
        if (!start.isBefore(end) || attendees < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "start must be before end and attendees >= 1");
        }
        Set<Long> busy = bookings.findAllConfirmedInWindow(start, end).stream()
                .map(b -> b.getRoom().getId())
                .collect(Collectors.toSet());
        return rooms.findByActiveTrue().stream()
                .filter(r -> r.getCapacity() >= attendees && !busy.contains(r.getId()))
                .sorted(Comparator.comparingInt(Room::getCapacity).thenComparing(Room::getName))
                .toList();
    }
}
