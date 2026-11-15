package com.example.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Graded after the attempt. Rules from the README that the other tests do not cover. */
class HiddenSpecGapsTest extends ApiTestBase {

    private org.springframework.test.web.servlet.ResultActions raw(String roomId, String title, String start, String end) throws Exception {
        String json = """
                {"roomId": %s, "organizerEmail": "ana@example.com", "title": %s,
                 "start": "%s", "end": "%s", "attendees": 2}
                """.formatted(roomId, title, start, end);
        return mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void blankTitleIs400() throws Exception {
        raw(oslo.getId().toString(), "\"   \"", "2026-03-03T10:00:00", "2026-03-03T11:00:00")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void titleOver100Is400() throws Exception {
        raw(oslo.getId().toString(), "\"" + "x".repeat(101) + "\"", "2026-03-03T10:00:00", "2026-03-03T11:00:00")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void missingRoomIdIs400() throws Exception {
        raw("null", "\"x\"", "2026-03-03T10:00:00", "2026-03-03T11:00:00")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void nonZeroSecondsIs400() throws Exception {
        raw(oslo.getId().toString(), "\"x\"", "2026-03-03T10:00:30", "2026-03-03T11:00:00")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void inactiveCheckedBeforeCapacity() throws Exception {
        postBooking(kyoto.getId(), "ana@example.com", tomorrow(10, 0), tomorrow(11, 0), 9)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ROOM_INACTIVE"));
    }

    @Test
    void availabilityStartEqualsEndIs400() throws Exception {
        mvc.perform(get("/rooms/available").param("start", "2026-03-03T10:00:00")
                        .param("end", "2026-03-03T10:00:00").param("attendees", "1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    /** Different but overlapping slots, through the API: one 201, the rest 409 BOOKING_CONFLICT. */
    @RepeatedTest(5)
    void overlappingSlotsFromManyThreadsThroughApi() throws Exception {
        int threads = 8;
        CountDownLatch go = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                int startMin = (i % 4) * 15;   // 10:00, 10:15, 10:30, 10:45 → all overlap 10:45–11:00
                String email = "user" + i + "@example.com";
                results.add(pool.submit(() -> {
                    go.await();
                    var res = postBooking(oslo.getId(), email, tomorrow(10, startMin), tomorrow(11, startMin), 2)
                            .andReturn().getResponse();
                    return res.getStatus() == 201 ? "201" : res.getStatus() + " " + res.getContentAsString();
                }));
            }
            go.countDown();
        }
        List<String> out = new ArrayList<>();
        for (Future<String> r : results) out.add(r.get());
        assertThat(out).filteredOn("201"::equals).hasSize(1);
        assertThat(out).filteredOn(s -> !s.equals("201")).allMatch(s -> s.startsWith("409") && s.contains("BOOKING_CONFLICT"));
    }
}
