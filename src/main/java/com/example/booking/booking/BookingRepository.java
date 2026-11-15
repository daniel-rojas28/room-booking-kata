package com.example.booking.booking;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = "room")
    Page<Booking> findByOrganizerEmail(String organizerEmail, Pageable pageable);

    @Query("""
            select count(b) > 0 from Booking b
            where b.room.id = :roomId and b.status = com.example.booking.booking.BookingStatus.CONFIRMED
              and b.start < :end and b.end > :start""")
    boolean existsOverlap(Long roomId, LocalDateTime start, LocalDateTime end);

    @Query("""
            select b from Booking b join fetch b.room
            where b.room.id = :roomId and b.status = com.example.booking.booking.BookingStatus.CONFIRMED
              and b.start < :to and b.end > :from
            order by b.start""")
    List<Booking> findConfirmedInWindow(Long roomId, LocalDateTime from, LocalDateTime to);

    @Query("""
            select b from Booking b join fetch b.room
            where b.status = com.example.booking.booking.BookingStatus.CONFIRMED
              and b.start < :to and b.end > :from""")
    List<Booking> findAllConfirmedInWindow(LocalDateTime from, LocalDateTime to);
}
