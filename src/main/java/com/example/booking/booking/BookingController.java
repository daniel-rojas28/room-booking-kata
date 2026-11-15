package com.example.booking.booking;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> byOrganizer(@RequestParam String organizer,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Page<BookingResponse> result = service.byOrganizer(organizer, page, size);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> get(@PathVariable Long id,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        BookingResponse booking = service.get(id);
        if (booking.etag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(booking.etag()).build();
        }
        return ResponseEntity.ok().eTag(booking.etag()).body(booking);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BookingResponse> update(@PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @Valid @RequestBody UpdateBookingRequest request) {
        BookingResponse updated = service.update(id, userEmail, ifMatch, request);
        return ResponseEntity.ok().eTag(updated.etag()).body(updated);
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        BookingResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/bookings/" + created.id())).body(created);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable Long id, @RequestHeader("X-User-Email") String userEmail) {
        BookingResponse cancelled = service.cancel(id, userEmail);
        return ResponseEntity.ok().eTag(cancelled.etag()).body(cancelled);
    }
}
