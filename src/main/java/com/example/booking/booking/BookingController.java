package com.example.booking.booking;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public BookingResponse create(@RequestBody CreateBookingRequest request) {
        return service.create(request);
    }
}
