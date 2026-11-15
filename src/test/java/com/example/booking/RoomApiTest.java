package com.example.booking;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class RoomApiTest extends ApiTestBase {

    @Test
    void listsAllRooms() throws Exception {
        mvc.perform(get("/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void getsOneRoom() throws Exception {
        mvc.perform(get("/rooms/{id}", oslo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Oslo"))
                .andExpect(jsonPath("$.capacity").value(8));
    }

    @Test
    void unknownRoomIs404() throws Exception {
        mvc.perform(get("/rooms/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"));
    }
}
