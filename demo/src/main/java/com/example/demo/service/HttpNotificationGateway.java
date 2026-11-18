package com.example.demo.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.demo.dto.NotificationDTO;

@Service
public class HttpNotificationGateway implements NotificationGateway {

    private static final Logger logger =
        LoggerFactory.getLogger(HttpNotificationGateway.class);

    private final RestClient restClient;

    public HttpNotificationGateway() {

        SimpleClientHttpRequestFactory requestFactory =
            new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(
            Duration.ofSeconds(2)
        );

        requestFactory.setReadTimeout(
            Duration.ofSeconds(2)
        );

        this.restClient =
            RestClient.builder()
                .baseUrl("http://localhost:8080")
                .requestFactory(requestFactory)
                .build();
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
                    "Notification sent successfully: recipient={}, type={}",
                    request.recipient(),
                    request.type()
                );

                return;

            } catch (Exception e) {

                logger.warn(
                    "Notification attempt {} of {} failed for recipient={}: {}",
                    attempt,
                    maxAttempts,
                    request.recipient(),
                    e.getMessage()
                );
            }
        }

        logger.warn(
            "Notification failed after {} attempts for recipient={}. " +
            "Booking or cancellation remains committed.",
            maxAttempts,
            request.recipient()
        );
    }
}