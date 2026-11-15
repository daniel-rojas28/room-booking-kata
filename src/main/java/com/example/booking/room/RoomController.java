package com.example.booking.room;

import com.example.booking.booking.BookingResponse;
import com.example.booking.booking.BookingService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;
    private final BookingService bookingService;

    public RoomController(RoomService roomService, BookingService bookingService) {
        this.roomService = roomService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<Room> all() {
        return roomService.all();
    }

    @GetMapping("/{id}")
    public Room get(@PathVariable Long id) {
        return roomService.get(id);
    }

    @GetMapping("/available")
    public List<Room> available(@RequestParam LocalDateTime start, @RequestParam LocalDateTime end,
            @RequestParam int attendees) {
        return roomService.available(start, end, attendees);
    }

    @GetMapping("/{id}/bookings")
    public List<BookingResponse> bookings(@PathVariable Long id, @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {
        return bookingService.roomBookings(id, from, to);
    }
}
