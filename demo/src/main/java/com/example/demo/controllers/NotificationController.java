package com.example.demo.controllers;

import com.example.demo.dto.NotificationDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mock-notifications")
public class NotificationController {

    private static final Logger logger =
        LoggerFactory.getLogger(NotificationController.class);

    @PostMapping
    public ResponseEntity<Void> send(
            @RequestBody NotificationDTO request) {

        logger.info(
            "Mock notification received: recipient={}, type={}, message={}",
            request.recipient(),
            request.type(),
            request.message()
        );

        return ResponseEntity.ok().build();
    }
}