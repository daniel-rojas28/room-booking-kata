package com.example.booking.booking;

import com.example.booking.room.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Room room;

    @Column(nullable = false)
    private String organizerEmail;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime start;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime end;

    private int attendees;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;

    protected Booking() {
    }

    public Booking(Room room, String organizerEmail, String title, LocalDateTime start, LocalDateTime end, int attendees) {
        this.room = room;
        this.organizerEmail = organizerEmail;
        this.title = title;
        this.start = start;
        this.end = end;
        this.attendees = attendees;
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public String getOrganizerEmail() {
        return organizerEmail;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public int getAttendees() {
        return attendees;
    }

    public BookingStatus getStatus() {
        return status;
    }
}
