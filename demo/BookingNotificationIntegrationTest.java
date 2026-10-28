package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.example.demo.dto.NotificationRequest;
import com.example.demo.service.BookingService;
import com.example.demo.service.NotificationGateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class BookingNotificationIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @MockitoBean
    private NotificationGateway notificationGateway;

    @Test
    void bookingStillSucceedsWhenNotificationFails() {

        doThrow(new RuntimeException("Notification service down"))
            .when(notificationGateway)
            .send(any(NotificationRequest.class));

        assertDoesNotThrow(() -> {

            bookingService.createBooking(
                1L,
                1L,
                "Test booking"
            );

            try {
                notificationGateway.send(
                    new NotificationRequest(
                        "testuser",
                        "BOOKING_CONFIRMATION",
                        "Booking confirmed"
                    )
                );
            } catch (Exception ignored) {
            }
        });
    }
}