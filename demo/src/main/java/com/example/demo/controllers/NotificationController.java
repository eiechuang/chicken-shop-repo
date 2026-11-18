package com.example.demo.controllers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.NotificationDTO;

@RestController
@RequestMapping("/mock-notifications")
public class NotificationController {

    private static final Logger logger =
        LoggerFactory.getLogger(NotificationController.class);

    private final Set<String> processedEventIds =
        ConcurrentHashMap.newKeySet();

    @PostMapping
    public ResponseEntity<Void> send(
            @RequestBody NotificationDTO request) {

        if (!processedEventIds.add(request.eventId())) {

            logger.warn(
                "Duplicate notification ignored: eventId={}",
                request.eventId()
            );

            return ResponseEntity.ok().build();
        }

        logger.info(
            "Mock notification received: eventId={}, recipient={}, type={}, message={}",
            request.eventId(),
            request.recipient(),
            request.type(),
            request.message()
        );

        return ResponseEntity.ok().build();
    }
}