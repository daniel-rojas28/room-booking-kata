package com.example.booking.report;

import com.example.booking.booking.Booking;
import com.example.booking.booking.BookingRepository;
import com.example.booking.room.RoomRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    static final LocalTime OPEN = LocalTime.of(8, 0);
    static final LocalTime CLOSE = LocalTime.of(18, 0);

    private final RoomRepository rooms;
    private final BookingRepository bookings;

    public ReportService(RoomRepository rooms, BookingRepository bookings) {
        this.rooms = rooms;
        this.bookings = bookings;
    }

    @Transactional(readOnly = true)
    public List<RoomUtilization> utilization(LocalDate date) {
        LocalDateTime open = date.atTime(OPEN);
        LocalDateTime close = date.atTime(CLOSE);
        long dayMinutes = Duration.between(open, close).toMinutes();
        Map<Long, Long> minutesByRoom = bookings.findAllConfirmedInWindow(open, close).stream()
                .collect(Collectors.groupingBy(b -> b.getRoom().getId(),
                        Collectors.summingLong(b -> overlapMinutes(b, open, close))));
        return rooms.findByActiveTrue().stream()
                .map(r -> {
                    long minutes = minutesByRoom.getOrDefault(r.getId(), 0L);
                    double pct = BigDecimal.valueOf(minutes * 100.0 / dayMinutes)
                            .setScale(1, RoundingMode.HALF_UP).doubleValue();
                    return new RoomUtilization(r.getId(), r.getName(), minutes, pct);
                })
                .sorted(Comparator.comparingDouble(RoomUtilization::utilizationPercent).reversed()
                        .thenComparing(RoomUtilization::roomName))
                .toList();
    }

    private static long overlapMinutes(Booking b, LocalDateTime open, LocalDateTime close) {
        LocalDateTime from = b.getStart().isAfter(open) ? b.getStart() : open;
        LocalDateTime to = b.getEnd().isBefore(close) ? b.getEnd() : close;
        return Duration.between(from, to).toMinutes();
    }
}
