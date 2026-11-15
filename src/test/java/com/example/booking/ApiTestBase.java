package com.example.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.booking.booking.Booking;
import com.example.booking.booking.BookingRepository;
import com.example.booking.room.Room;
import com.example.booking.room.RoomRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfig.class)
public abstract class ApiTestBase {

    /** Tomorrow (Tuesday 3 March 2026) at the given time. */
    protected static LocalDateTime tomorrow(int hour, int minute) {
        return FixedClockConfig.NOW.toLocalDate().plusDays(1).atTime(hour, minute);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected RoomRepository rooms;

    @Autowired
    protected BookingRepository bookings;

    protected Room lisbon;   // capacity 4
    protected Room oslo;     // capacity 8
    protected Room quito;    // capacity 12
    protected Room kyoto;    // capacity 6, inactive

    @BeforeEach
    void resetData() {
        bookings.deleteAll();
        rooms.deleteAll();
        lisbon = rooms.save(new Room("Lisbon", 1, 4, true));
        oslo = rooms.save(new Room("Oslo", 1, 8, true));
        quito = rooms.save(new Room("Quito", 2, 12, true));
        kyoto = rooms.save(new Room("Kyoto", 2, 6, false));
    }

    /** Inserts a booking straight into the database, skipping the API rules. */
    protected Booking existing(Room room, String email, LocalDateTime start, LocalDateTime end) {
        return bookings.save(new Booking(room, email, "Existing meeting", start, end, 2));
    }

    protected ResultActions postBooking(Long roomId, String email, LocalDateTime start, LocalDateTime end,
            int attendees) throws Exception {
        String json = """
                {"roomId": %d, "organizerEmail": "%s", "title": "Planning",
                 "start": "%s", "end": "%s", "attendees": %d}
                """.formatted(roomId, email, start, end, attendees);
        return mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
