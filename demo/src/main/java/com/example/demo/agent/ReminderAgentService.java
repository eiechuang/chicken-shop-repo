package com.example.demo.agent;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.dto.BookingDTO;
import com.example.demo.dto.NotificationDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.service.BookingService;
import com.example.demo.service.NotificationGateway;
import com.example.demo.service.UserService;

@Service
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class ReminderAgentService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderAgentService.class);

    private final BookingService bookingService;
    private final UserService userService;
    private final NotificationGateway notificationGateway;
    private final ChatClient reminderChatClient;

    public ReminderAgentService(
            BookingService bookingService,
            UserService userService,
            NotificationGateway notificationGateway,
            @Qualifier("reminderChatClient") ChatClient reminderChatClient) {
        this.bookingService = bookingService;
        this.userService = userService;
        this.notificationGateway = notificationGateway;
        this.reminderChatClient = reminderChatClient;
    }

    @Scheduled(cron = "${app.reminders.cron:0 0 9 * * *}")
    public void scheduledRun() {
        runReminderScan();
    }

    public int runReminderScan() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plusHours(24);

        List<BookingDTO> upcoming = bookingService.getAllBookings().stream()
            .filter(b -> "BOOKED".equals(b.status()))
            .filter(b -> !b.startTime().isBefore(now) && !b.startTime().isAfter(cutoff))
            .toList();

        int sent = 0;
        for (BookingDTO booking : upcoming) {
            UserDTO user = userService.getUserById(booking.userId());
            if (user == null) {
                logger.warn("Skipping reminder because user {} was not found", booking.userId());
                continue;
            }

            String prompt = "Write a concise pickup reminder for customer " + user.username()
                + ". Their pickup starts at " + booking.startTime()
                + " and ends at " + booking.endTime()
                + ". Mention order " + booking.orderId() + ".";

            String message = reminderChatClient.prompt()
                .user(prompt)
                .call()
                .content();

            notificationGateway.send(new NotificationDTO(
                "REMINDER-" + booking.orderId(),
                user.username(),
                "PICKUP_REMINDER",
                message == null ? "Your pickup is coming up soon." : message
            ));
            sent++;
        }

        logger.info("Autonomous reminder scan complete: {} reminder(s) sent", sent);
        return sent;
    }
}
