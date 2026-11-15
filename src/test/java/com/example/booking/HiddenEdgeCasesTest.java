package com.example.booking;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.booking.booking.Booking;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Hidden spec cases, run by grade.sh. */
class HiddenEdgeCasesTest extends ApiTestBase {

    @Nested
    class Create {

        @Test
        void backToBackIsAllowed() throws Exception {
            existing(oslo, "bob@example.com", tomorrow(10, 0), tomorrow(11, 0));
            postBooking(oslo.getId(), "ana@example.com", tomorrow(11, 0), tomorrow(12, 0), 2)
                    .andExpect(status().isCreated());
            postBooking(oslo.getId(), "ana@example.com", tomorrow(9, 0), tomorrow(10, 0), 2)
                    .andExpect(status().isCreated());
        }

        @Test
        void bookingInsideAnotherIsConflict() throws Exception {
            existing(oslo, "bob@example.com", tomorrow(9, 0), tomorrow(12, 0));
            postBooking(oslo.getId(), "ana@example.com", tomorrow(10, 0), tomorrow(10, 30), 2)
                    .andExpect(status().isConflict());
        }

        @Test
        void bookingCoveringAnotherIsConflict() throws Exception {
            existing(oslo, "bob@example.com", tomorrow(10, 0), tomorrow(10, 30));
            postBooking(oslo.getId(), "ana@example.com", tomorrow(9, 0), tomorrow(12, 0), 2)
                    .andExpect(status().isConflict());
        }

        @Test
        void cancelledBookingDoesNotBlock() throws Exception {
            Booking b = existing(oslo, "bob@example.com", tomorrow(10, 0), tomorrow(11, 0));
            b.cancel();
            bookings.save(b);
            postBooking(oslo.getId(), "ana@example.com", tomorrow(10, 0), tomorrow(11, 0), 2)
                    .andExpect(status().isCreated());
        }

        @Test
        void sameTimeOtherRoomIsFine() throws Exception {
            existing(quito, "bob@example.com", tomorrow(10, 0), tomorrow(11, 0));
            postBooking(oslo.getId(), "ana@example.com", tomorrow(10, 0), tomorrow(11, 0), 2)
                    .andExpect(status().isCreated());
        }

        @Test
        void startEqualsEndIs400() throws Exception {
            postBooking(oslo.getId(), "ana@example.com", tomorrow(10, 0), tomorrow(10, 0), 2)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void startInThePastIs400() throws Exception {
            postBooking(oslo.getId(), "ana@example.com", FixedClockConfig.NOW.minusHours(1), FixedClockConfig.NOW.plusHours(1), 2)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void startingRightNowIsAllowed() throws Exception {
            postBooking(oslo.getId(), "ana@example.com", FixedClockConfig.NOW, FixedClockConfig.NOW.plusHours(1), 2)
                    .andExpect(status().isCreated());
        }

        @Test
        void notOnQuarterHourIs400() throws Exception {
            postBooking(oslo.getId(), "ana@example.com", tomorrow(10, 10), tomorrow(11, 0), 2)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void exactlyFourHoursIsAllowed() throws Exception {
            postBooking(oslo.getId(), "ana@example.com", tomorrow(9, 0), tomorrow(13, 0), 2)
                    .andExpect(status().isCreated());
        }

        @Test
        void overFourHoursIs400() throws Exception {
            postBooking(oslo.getId(), "ana@example.com", tomorrow(9, 0), tomorrow(13, 15), 2)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void attendeesEqualToCapacityIsAllowed() throws Exception {
            postBooking(lisbon.getId(), "ana@example.com", tomorrow(10, 0), tomorrow(11, 0), 4)
                    .andExpect(status().isCreated());
        }

        @Test
        void zeroAttendeesIs400() throws Exception {
            postBooking(oslo.getId(), "ana@example.com", tomorrow(10, 0), tomorrow(11, 0), 0)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void badEmailIs400() throws Exception {
            postBooking(oslo.getId(), "not-an-email", tomorrow(10, 0), tomorrow(11, 0), 2)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void inactiveRoomIs409() throws Exception {
            postBooking(kyoto.getId(), "ana@example.com", tomorrow(10, 0), tomorrow(11, 0), 2)
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ROOM_INACTIVE"));
        }

        @Test
        void validationComesBeforeRoomLookup() throws Exception {
            postBooking(999_999L, "ana@example.com", tomorrow(11, 0), tomorrow(10, 0), 2)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void capacityCheckedBeforeConflict() throws Exception {
            existing(lisbon, "bob@example.com", tomorrow(10, 0), tomorrow(11, 0));
            postBooking(lisbon.getId(), "ana@example.com", tomorrow(10, 0), tomorrow(11, 0), 9)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("CAPACITY_EXCEEDED"));
        }

        @Test
        void malformedJsonIs400() throws Exception {
            mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content("{\"roomId\": "))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void badDateFormatIs400() throws Exception {
            String json = """
                    {"roomId": %d, "organizerEmail": "ana@example.com", "title": "x",
                     "start": "tomorrow", "end": "2026-03-03T11:00:00", "attendees": 2}
                    """.formatted(oslo.getId());
            mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void createdBookingCanBeFetched() throws Exception {
            String location = postBooking(oslo.getId(), "ana@example.com", tomorrow(10, 0), tomorrow(11, 0), 2)
                    .andReturn().getResponse().getHeader("Location");
            mvc.perform(get(location)).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Planning"));
        }
    }

    @Nested
    class Cancel {

        @Test
        void organizerEmailIsCaseInsensitive() throws Exception {
            String location = postBooking(oslo.getId(), "Ana@Example.com", tomorrow(10, 0), tomorrow(11, 0), 2)
                    .andReturn().getResponse().getHeader("Location");
            mvc.perform(post(location + "/cancel").header("X-User-Email", "ana@EXAMPLE.com"))
                    .andExpect(status().isOk());
        }

        @Test
        void startedBookingIs409() throws Exception {
            Booking b = existing(oslo, "ana@example.com", FixedClockConfig.NOW.minusMinutes(30), FixedClockConfig.NOW.plusMinutes(30));
            mvc.perform(post("/bookings/{id}/cancel", b.getId()).header("X-User-Email", "ana@example.com"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("BOOKING_STARTED"));
        }

        @Test
        void bookingStartingNowCountsAsStarted() throws Exception {
            Booking b = existing(oslo, "ana@example.com", FixedClockConfig.NOW, FixedClockConfig.NOW.plusHours(1));
            mvc.perform(post("/bookings/{id}/cancel", b.getId()).header("X-User-Email", "ana@example.com"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("BOOKING_STARTED"));
        }

        @Test
        void notOrganizerCheckedBeforeAlreadyCancelled() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            b.cancel();
            bookings.save(b);
            mvc.perform(post("/bookings/{id}/cancel", b.getId()).header("X-User-Email", "bob@example.com"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void missingHeaderIs400() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            mvc.perform(post("/bookings/{id}/cancel", b.getId()))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void cancelledSlotCanBeBookedAgain() throws Exception {
            Booking b = existing(oslo, "ana@example.com", tomorrow(10, 0), tomorrow(11, 0));
            mvc.perform(post("/bookings/{id}/cancel", b.getId()).header("X-User-Email", "ana@example.com"));
            postBooking(oslo.getId(), "bob@example.com", tomorrow(10, 0), tomorrow(11, 0), 2)
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    class Availability {

        @Test
        void excludesInactiveAndTooSmallRooms() throws Exception {
            mvc.perform(get("/rooms/available").param("start", "2026-03-03T10:00:00")
                            .param("end", "2026-03-03T11:00:00").param("attendees", "5"))
                    .andExpect(jsonPath("$[*].name", contains("Oslo", "Quito")));
        }

        @Test
        void backToBackRoomIsAvailable() throws Exception {
            existing(lisbon, "bob@example.com", tomorrow(9, 0), tomorrow(10, 0));
            mvc.perform(get("/rooms/available").param("start", "2026-03-03T10:00:00")
                            .param("end", "2026-03-03T11:00:00").param("attendees", "1"))
                    .andExpect(jsonPath("$[*].name", contains("Lisbon", "Oslo", "Quito")));
        }

        @Test
        void cancelledBookingDoesNotBlock() throws Exception {
            Booking b = existing(lisbon, "bob@example.com", tomorrow(10, 0), tomorrow(11, 0));
            b.cancel();
            bookings.save(b);
            mvc.perform(get("/rooms/available").param("start", "2026-03-03T10:00:00")
                            .param("end", "2026-03-03T11:00:00").param("attendees", "1"))
                    .andExpect(jsonPath("$[*].name", contains("Lisbon", "Oslo", "Quito")));
        }

        @Test
        void sameCapacitySortedByName() throws Exception {
            rooms.save(new com.example.booking.room.Room("Athens", 3, 8, true));
            mvc.perform(get("/rooms/available").param("start", "2026-03-03T10:00:00")
                            .param("end", "2026-03-03T11:00:00").param("attendees", "6"))
                    .andExpect(jsonPath("$[*].name", contains("Athens", "Oslo", "Quito")));
        }

        @Test
        void nothingFitsIsEmptyList() throws Exception {
            mvc.perform(get("/rooms/available").param("start", "2026-03-03T10:00:00")
                            .param("end", "2026-03-03T11:00:00").param("attendees", "50"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", empty()));
        }

        @Test
        void missingParamIs400() throws Exception {
            mvc.perform(get("/rooms/available").param("start", "2026-03-03T10:00:00").param("attendees", "1"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void zeroAttendeesIs400() throws Exception {
            mvc.perform(get("/rooms/available").param("start", "2026-03-03T10:00:00")
                            .param("end", "2026-03-03T11:00:00").param("attendees", "0"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    class Report {

        @Test
        void clipsBookingsToBusinessHours() throws Exception {
            existing(oslo, "a@example.com", tomorrow(7, 0), tomorrow(9, 0));     // 60 in window
            existing(oslo, "b@example.com", tomorrow(17, 30), tomorrow(19, 0));  // 30 in window
            mvc.perform(get("/reports/utilization").param("date", "2026-03-03"))
                    .andExpect(jsonPath("$[0].roomName").value("Oslo"))
                    .andExpect(jsonPath("$[0].bookedMinutes").value(90))
                    .andExpect(jsonPath("$[0].utilizationPercent").value(15.0));
        }

        @Test
        void ignoresOtherDaysAndCancelled() throws Exception {
            existing(oslo, "a@example.com", tomorrow(10, 0).plusDays(1), tomorrow(11, 0).plusDays(1));
            Booking c = existing(oslo, "b@example.com", tomorrow(10, 0), tomorrow(11, 0));
            c.cancel();
            bookings.save(c);
            mvc.perform(get("/reports/utilization").param("date", "2026-03-03"))
                    .andExpect(jsonPath("$[*].bookedMinutes", contains(0, 0, 0)));
        }

        @Test
        void roundsToOneDecimalHalfUp() throws Exception {
            existing(oslo, "a@example.com", tomorrow(10, 0), tomorrow(10, 45));  // 45/600 = 7.5
            existing(quito, "b@example.com", tomorrow(10, 0), tomorrow(10, 15)); // 15/600 = 2.5
            mvc.perform(get("/reports/utilization").param("date", "2026-03-03"))
                    .andExpect(jsonPath("$[0].utilizationPercent").value(7.5))
                    .andExpect(jsonPath("$[1].utilizationPercent").value(2.5));
        }

        @Test
        void roundingOfThirds() throws Exception {
            existing(oslo, "a@example.com", tomorrow(10, 0), tomorrow(14, 0));   // 240
            existing(oslo, "b@example.com", tomorrow(14, 0), tomorrow(14, 15));  // +15 = 255 → 42.5
            existing(quito, "c@example.com", tomorrow(10, 0), tomorrow(12, 0));  // 120 → 20.0
            existing(lisbon, "d@example.com", tomorrow(10, 0), tomorrow(10, 0).plusMinutes(1)); // 1/600 = 0.1666 → 0.2
            mvc.perform(get("/reports/utilization").param("date", "2026-03-03"))
                    .andExpect(jsonPath("$[*].utilizationPercent", contains(42.5, 20.0, 0.2)));
        }

        @Test
        void tiesSortedByName() throws Exception {
            mvc.perform(get("/reports/utilization").param("date", "2026-03-03"))
                    .andExpect(jsonPath("$[*].roomName", contains("Lisbon", "Oslo", "Quito")));
        }

        @Test
        void badDateIs400() throws Exception {
            mvc.perform(get("/reports/utilization").param("date", "03/03/2026"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    class RoomBookings {

        @Test
        void unknownRoomIs404() throws Exception {
            mvc.perform(get("/rooms/{id}/bookings", 999_999).param("from", "2026-03-03T08:00:00")
                            .param("to", "2026-03-03T18:00:00"))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"));
        }

        @Test
        void partialOverlapIsIncludedAndCancelledIsNot() throws Exception {
            existing(oslo, "a@example.com", tomorrow(7, 0), tomorrow(8, 30));
            Booking c = existing(oslo, "b@example.com", tomorrow(10, 0), tomorrow(11, 0));
            c.cancel();
            bookings.save(c);
            existing(oslo, "c@example.com", tomorrow(18, 0), tomorrow(19, 0));   // touches the end only
            mvc.perform(get("/rooms/{id}/bookings", oslo.getId()).param("from", "2026-03-03T08:00:00")
                            .param("to", "2026-03-03T18:00:00"))
                    .andExpect(jsonPath("$[*].organizerEmail", contains("a@example.com")));
        }
    }
}
