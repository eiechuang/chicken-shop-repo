package com.example.demo.controllers;

import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.agent.ReminderAgentService;

@RestController
@RequestMapping("/agent/reminders")
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class ReminderAgentController {

    private final ReminderAgentService reminderAgentService;

    public ReminderAgentController(ReminderAgentService reminderAgentService) {
        this.reminderAgentService = reminderAgentService;
    }

    @PostMapping("/run")
    public Map<String, Object> run(HttpSession session, HttpServletResponse response) {
        String role = (String) session.getAttribute("role");
        if (role == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Map.of("error", "Login is required.");
        }
        if (!"PROVIDER".equals(role)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return Map.of("error", "Provider role is required for the manual reminder trigger.");
        }
        int sent = reminderAgentService.runReminderScan();
        return Map.of("remindersSent", sent);
    }
}
