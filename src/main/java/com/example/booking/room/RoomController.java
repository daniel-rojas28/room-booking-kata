package com.example.booking.room;

import com.example.booking.booking.BookingResponse;
import com.example.booking.booking.BookingService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public List<Room> all() {
        return roomRepository.findAll();
    }

    @GetMapping("/{id}")
    public Room get(@PathVariable Long id) {
        return roomRepository.findById(id).get();
    }

    @GetMapping("/{id}/bookings")
    public List<BookingResponse> bookings(@PathVariable Long id, @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {
        return bookingService.roomBookings(id, from, to);
    }
}
