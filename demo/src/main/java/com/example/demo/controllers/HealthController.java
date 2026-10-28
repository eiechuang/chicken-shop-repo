package com.example.demo.controllers;

import java.util.Map;

import com.example.demo.service.BookingService;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final BookingService bookingService;

    public HealthController(
            JdbcTemplate jdbcTemplate,
            BookingService bookingService) {

        this.jdbcTemplate = jdbcTemplate;
        this.bookingService = bookingService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {

        boolean databaseUp;

        try {
            jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
            );

            databaseUp = true;

        } catch (Exception e) {
            databaseUp = false;
        }

        return Map.of(
            "status",
            databaseUp ? "UP" : "DOWN",
            "database",
            databaseUp ? "UP" : "DOWN"
        );
    }

    @GetMapping("/metrics")
    public Map<String, Integer> metrics() {

        return Map.of(
            "successfulBookings",
            bookingService.getBookingCount()
        );
    }
}