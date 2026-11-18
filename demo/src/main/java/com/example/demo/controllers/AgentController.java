package com.example.demo.controllers;

import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.agent.BookingAgentTools;
import com.example.demo.agent.PendingActionStore;
import com.example.demo.service.BookingService;
import com.example.demo.service.NotificationGateway;
import com.example.demo.service.SlotService;

@RestController
@RequestMapping("/agent")
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
public class AgentController {

    public record ChatRequest(String message) {}

    private final ChatClient chatClient;
    private final VectorStore policyVectorStore;
    private final SlotService slotService;
    private final BookingService bookingService;
    private final NotificationGateway notificationGateway;
    private final PendingActionStore pendingActions;

    public AgentController(
            @Qualifier("agentChatClient") ChatClient agentChatClient,
            VectorStore policyVectorStore,
            SlotService slotService,
            BookingService bookingService,
            NotificationGateway notificationGateway,
            PendingActionStore pendingActions) {
        this.chatClient = agentChatClient;
        this.policyVectorStore = policyVectorStore;
        this.slotService = slotService;
        this.bookingService = bookingService;
        this.notificationGateway = notificationGateway;
        this.pendingActions = pendingActions;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(
            @RequestBody ChatRequest request,
            HttpSession session,
            HttpServletResponse response) {

        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");

        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Map.of("error", "Login is required.");
        }
        if (!"CUSTOMER".equals(role)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return Map.of("error", "The conversational agent acts only as a customer.");
        }
        if (request == null || request.message() == null || request.message().isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return Map.of("error", "message is required");
        }

        String conversationId = session.getId();
        String turnId = UUID.randomUUID().toString();
        BookingAgentTools tools = new BookingAgentTools(
            userId, username, role, conversationId, turnId, request.message(),
            slotService, bookingService, notificationGateway, pendingActions
        );

        String answer = chatClient.prompt()
            .user(request.message())
            .tools(tools)
            .advisors(QuestionAnswerAdvisor.builder(policyVectorStore).build())
            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
            .call()
            .content();

        return Map.of("reply", answer == null ? "" : answer);
    }
}
