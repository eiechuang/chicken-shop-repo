package com.example.demo.service;

import com.example.demo.dto.NotificationDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class HttpNotificationGateway implements NotificationGateway {

    private static final Logger logger =
        LoggerFactory.getLogger(HttpNotificationGateway.class);

    private final RestClient restClient;

    public HttpNotificationGateway() {
        this.restClient =
            RestClient.create("http://localhost:8080");
    }

    @Override
    public void send(NotificationDTO request) {

        int maxAttempts = 3;

        for (int attempt = 1;
             attempt <= maxAttempts;
             attempt++) {

            try {

                restClient.post()
                    .uri("/mock-notifications")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

                logger.info(
                    "Notification sent successfully to {}",
                    request.recipient()
                );

                return;

            } catch (Exception e) {

                logger.warn(
                    "Notification attempt {} failed for {}",
                    attempt,
                    request.recipient()
                );
            }
        }

        logger.warn(
            "Notification failed after all retries; booking remains committed"
        );
    }
}