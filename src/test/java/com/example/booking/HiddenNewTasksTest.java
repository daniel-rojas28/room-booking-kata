package com.example.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.booking.booking.Booking;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

/** Spec cases for tasks 5, 6 and 9 that the visible tests don't cover. */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class HiddenNewTasksTest extends ApiTestBase {

    @Autowired
    EntityManagerFactory emf;

    ResultActions patchBooking(Long id, String user, String ifMatch, String json) throws Exception {
        return mvc.perform(patch("/bookings/{id}", id).header("X-User-Email", user).header("If-Match", ifMatch)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Nested
    class MyBookings {
        @Test
        void organizerIsCaseInsensitive() throws Exception {
            existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            mvc.perform(get("/bookings").param("organizer", "ANA@Example.com"))
                    .andExpect(header().string("X-Total-Count", "1")).andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void includesCancelledBookings() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            mvc.perform(post("/bookings/{id}/cancel", b.getId()).header("X-User-Email", "ana@example.com"));
            mvc.perform(get("/bookings").param("organizer", "ana@example.com"))
                    .andExpect(jsonPath("$[0].status").value("CANCELLED"));
        }

        @Test
        void defaultPageSizeIs20() throws Exception {
            for (int d = 0; d < 25; d++) {
                existing(oslo, "ana@example.com", tomorrow(10, 0).plusDays(d), tomorrow(11, 0).plusDays(d));
            }
            mvc.perform(get("/bookings").param("organizer", "ana@example.com"))
                    .andExpect(header().string("X-Total-Count", "25")).andExpect(jsonPath("$", hasSize(20)));
        }

        @Test
        void sameStartIsOrderedById() throws Exception {
            Booking first = existing(quito, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            Booking second = existing(lisbon, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            mvc.perform(get("/bookings").param("organizer", "ana@example.com"))
                    .andExpect(jsonPath("$[0].id").value(first.getId()))
                    .andExpect(jsonPath("$[1].id").value(second.getId()));
        }

        @Test
        void pageAfterTheEndIsEmptyWithTheTotal() throws Exception {
            existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            mvc.perform(get("/bookings").param("organizer", "ana@example.com").param("page", "3"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Total-Count", "1")).andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void badPagingIs400() throws Exception {
            for (String[] p : new String[][] {{"size", "0"}, {"size", "101"}, {"page", "-1"}, {"size", "abc"}}) {
                mvc.perform(get("/bookings").param("organizer", "ana@example.com").param(p[0], p[1]))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
            }
        }

        @Test
        void pagesInTheDatabaseWithoutNPlusOne() throws Exception {
            List<Booking> many = new ArrayList<>();
            for (int d = 0; d < 1000; d++) {
                var room = d % 3 == 0 ? oslo : d % 3 == 1 ? quito : lisbon;
                many.add(new Booking(room, "ana@example.com", "Many", tomorrow(10, 0).plusDays(d),
                        tomorrow(11, 0).plusDays(d), 2));
            }
            bookings.saveAll(many);

            Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
            stats.clear();
            mvc.perform(get("/bookings").param("organizer", "ana@example.com"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(20)));

            assertThat(stats.getEntityLoadCount()).as("entities loaded").isLessThanOrEqualTo(25);
            assertThat(stats.getPrepareStatementCount()).as("SQL statements").isLessThanOrEqualTo(2);
        }
    }

    @Nested
    class Edit {
        @Test
        void currentETagGives304WithoutBody() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            mvc.perform(get("/bookings/{id}", b.getId()).header("If-None-Match", "\"0\""))
                    .andExpect(status().isNotModified())
                    .andExpect(header().string("ETag", "\"0\""))
                    .andExpect(content().string(""));
        }

        @Test
        void attendeesOnlyKeepsTheTitle() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            patchBooking(b.getId(), "ana@example.com", "\"0\"", "{\"attendees\": 6}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attendees").value(6))
                    .andExpect(jsonPath("$.title").value("Existing meeting"));
        }

        @Test
        void invalidFieldsAre400() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            for (String json : List.of("{\"title\": \"  \"}", "{\"title\": \"" + "x".repeat(101) + "\"}",
                    "{\"attendees\": 0}", "{\"attendees\": \"many\"}")) {
                patchBooking(b.getId(), "ana@example.com", "\"0\"", json)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
            }
        }

        @Test
        void missingUserIs400() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            mvc.perform(patch("/bookings/{id}", b.getId()).header("If-Match", "\"0\"")
                            .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Retro\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void unknownBookingIs404() throws Exception {
            patchBooking(999_999L, "ana@example.com", "\"0\"", "{\"title\": \"Retro\"}")
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("BOOKING_NOT_FOUND"));
        }

        @Test
        void organizerCheckComesBeforeTheETag() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            patchBooking(b.getId(), "bob@example.com", "\"9\"", "{\"title\": \"Retro\"}")
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("NOT_ORGANIZER"));
        }

        @Test
        void organizerIsCaseInsensitive() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            patchBooking(b.getId(), "Ana@EXAMPLE.com", "\"0\"", "{\"title\": \"Retro\"}").andExpect(status().isOk());
        }

        @Test
        void eTagCheckComesBeforeTheCancelledCheck() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            mvc.perform(post("/bookings/{id}/cancel", b.getId()).header("X-User-Email", "ana@example.com"));
            patchBooking(b.getId(), "ana@example.com", "\"0\"", "{\"title\": \"Retro\"}")
                    .andExpect(status().isPreconditionFailed());
            patchBooking(b.getId(), "ana@example.com", "\"1\"", "{\"title\": \"Retro\"}")
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ALREADY_CANCELLED"));
        }

        @Test
        void cancelChangesTheETag() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            mvc.perform(post("/bookings/{id}/cancel", b.getId()).header("X-User-Email", "ana@example.com"))
                    .andExpect(status().isOk());
            mvc.perform(get("/bookings/{id}", b.getId())).andExpect(header().string("ETag", "\"1\""));
        }

        @Test
        void startedBookingIs409() throws Exception {
            Booking b = existing(oslo, "ana@example.com", FixedClockConfig.NOW, FixedClockConfig.NOW.plusHours(1));
            patchBooking(b.getId(), "ana@example.com", "\"0\"", "{\"title\": \"Retro\"}")
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("BOOKING_STARTED"));
        }

        @Test
        void tooManyAttendeesIs400() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            patchBooking(b.getId(), "ana@example.com", "\"0\"", "{\"attendees\": 9}")
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("CAPACITY_EXCEEDED"));
        }

        @Test
        void twoEditsInARow() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            patchBooking(b.getId(), "ana@example.com", "\"0\"", "{\"title\": \"One\"}")
                    .andExpect(header().string("ETag", "\"1\""));
            patchBooking(b.getId(), "ana@example.com", "\"0\"", "{\"title\": \"Lost update\"}")
                    .andExpect(status().isPreconditionFailed());
            patchBooking(b.getId(), "ana@example.com", "\"1\"", "{\"title\": \"Two\"}")
                    .andExpect(status().isOk()).andExpect(header().string("ETag", "\"2\""));
            mvc.perform(get("/bookings/{id}", b.getId())).andExpect(jsonPath("$.title").value("Two"));
        }
    }

    @Nested
    class RequestId {
        @Test
        void onValidationErrorsAndCreates() throws Exception {
            postBooking(oslo.getId(), "not-an-email", tomorrow(10, 0), tomorrow(11, 0), 2)
                    .andExpect(status().isBadRequest()).andExpect(header().exists("X-Request-Id"));
            postBooking(oslo.getId(), "ana@example.com", tomorrow(10, 0), tomorrow(11, 0), 2)
                    .andExpect(status().isCreated()).andExpect(header().string("X-Request-Id", matchesPattern("[0-9a-f-]{36}")));
        }

        @Test
        void blankIdIsReplaced() throws Exception {
            mvc.perform(get("/rooms").header("X-Request-Id", " "))
                    .andExpect(header().string("X-Request-Id", matchesPattern("[0-9a-f-]{36}")));
        }
    }
}
