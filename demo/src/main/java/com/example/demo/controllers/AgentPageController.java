package com.example.demo.controllers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
public class AgentPageController {

    @GetMapping("/agent")
    public String page(HttpSession session, HttpServletResponse response) {
        String role = (String) session.getAttribute("role");
        if (role == null) {
            return "redirect:/login?required";
        }
        if (!"CUSTOMER".equals(role)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return "403";
        }
        return "agent-chat";
    }
}
