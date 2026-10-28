package com.example.demo.dto;

public record NotificationDTO(
    String recipient,
    String type,
    String message
) {
}