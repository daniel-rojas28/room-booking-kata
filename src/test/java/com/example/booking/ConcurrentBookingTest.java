package com.example.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.booking.booking.BookingService;
import com.example.booking.booking.CreateBookingRequest;
import com.example.booking.booking.BookingStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

class ConcurrentBookingTest extends ApiTestBase {

    @Autowired
    BookingService service;

    @RepeatedTest(5)
    void sameSlotFromManyThreadsIsBookedOnce() throws Exception {
        int threads = 8;
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                String email = "user" + i + "@example.com";
                results.add(pool.submit(() -> {
                    go.await();
                    try {
                        service.create(new CreateBookingRequest(oslo.getId(), email, "Race",
                                tomorrow(10, 0), tomorrow(11, 0), 2));
                        return true;
                    } catch (RuntimeException e) {
                        return false;
                    }
                }));
            }
            go.countDown();
        }

        long succeeded = 0;
        for (Future<Boolean> r : results) {
            if (r.get()) {
                succeeded++;
            }
        }
        assertThat(succeeded).isEqualTo(1);
        assertThat(bookings.findAll()).filteredOn(b -> b.getStatus() == BookingStatus.CONFIRMED).hasSize(1);
    }
}
