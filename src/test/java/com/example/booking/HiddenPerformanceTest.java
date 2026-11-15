package com.example.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.booking.booking.Booking;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/** Task 8: the room bookings endpoint must not load the whole table or run one query per booking. */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class HiddenPerformanceTest extends ApiTestBase {

    @Autowired
    EntityManagerFactory emf;

    @Test
    void loadsOnlyWhatItReturnsInAFewQueries() throws Exception {
        List<Booking> noise = new ArrayList<>();
        for (int day = 0; day < 1000; day++) {
            noise.add(new Booking(quito, "x@example.com", "Noise", tomorrow(9, 0).plusDays(day), tomorrow(10, 0).plusDays(day), 2));
            noise.add(new Booking(oslo, "x@example.com", "Noise", tomorrow(9, 0).plusDays(day + 1), tomorrow(10, 0).plusDays(day + 1), 2));
        }
        bookings.saveAll(noise);
        for (int h = 9; h < 14; h++) {
            existing(oslo, "a@example.com", tomorrow(h, 0), tomorrow(h, 30));
        }

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        mvc.perform(get("/rooms/{id}/bookings", oslo.getId()).param("from", "2026-03-03T08:00:00")
                        .param("to", "2026-03-03T18:00:00"))
                .andExpect(status().isOk());

        assertThat(stats.getEntityLoadCount()).as("entities loaded").isLessThanOrEqualTo(10);
        assertThat(stats.getPrepareStatementCount()).as("SQL statements").isLessThanOrEqualTo(3);
    }
}
