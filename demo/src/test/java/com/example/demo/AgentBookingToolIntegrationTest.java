package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.demo.repo.BookingRepo;
import com.example.demo.repo.SlotRepo;
import com.example.demo.repo.UserRepo;
import com.example.demo.service.BookingService;

@SpringBootTest(properties = "app.ai.enabled=false")
class AgentBookingToolIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepo bookingRepo;

    @Autowired
    private SlotRepo slotRepo;

    @Autowired
    private UserRepo userRepo;

    @Test
    void agentBookingToolUsesTransactionalPathAndDuplicateGuard() {

        String username =
            "agent-test-" + System.nanoTime();

        userRepo.createUser(
            username,
            "test-hash",
            "CUSTOMER"
        );

        Long userId =
            userRepo.findByUsername(username).id();

        LocalDateTime start =
            LocalDateTime.now().plusDays(1);

        LocalDateTime end =
            start.plusMinutes(15);

        slotRepo.createSlot(start, end);

        Long slotId =
            slotRepo.findPage(10, 0)
                .stream()
                .filter(slot ->
                    slot.startTime().equals(start))
                .findFirst()
                .orElseThrow()
                .ID();

        bookingService.createBooking(
            userId,
            slotId,
            "Created through agent test"
        );

        assertEquals(
            1,
            bookingRepo.findByUserId(userId).size()
        );

        assertThrows(
            DataIntegrityViolationException.class,
            () -> bookingService.createBooking(
                userId,
                slotId,
                "Duplicate booking"
            )
        );

        assertEquals(
            1,
            bookingRepo.findByUserId(userId).size()
        );
    }
}