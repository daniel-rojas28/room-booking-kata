package com.example.booking;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class RequestIdTest extends ApiTestBase {

    @Test
    void echoesTheCallersId() throws Exception {
        mvc.perform(get("/rooms").header("X-Request-Id", "abc-123"))
                .andExpect(header().string("X-Request-Id", "abc-123"));
    }

    @Test
    void generatesOneOnErrorsToo() throws Exception {
        mvc.perform(get("/bookings/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Request-Id", matchesPattern("[0-9a-f-]{36}")));
    }
}
