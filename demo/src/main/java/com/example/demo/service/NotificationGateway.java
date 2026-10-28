package com.example.demo.service;


import com.example.demo.dto.NotificationDTO;

public interface NotificationGateway {
    void send(NotificationDTO request);
}