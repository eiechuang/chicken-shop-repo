package com.example.demo.agent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class PendingActionStore {

    public record PendingAction(String type, Long targetId, String preparedTurnId) {}

    private final ConcurrentMap<String, PendingAction> pending = new ConcurrentHashMap<>();

    public void prepareBooking(String conversationId, Long slotId, String turnId) {
        pending.put(conversationId, new PendingAction("BOOK", slotId, turnId));
    }

    public void prepareCancellation(String conversationId, Long orderId, String turnId) {
        pending.put(conversationId, new PendingAction("CANCEL", orderId, turnId));
    }

    public PendingAction get(String conversationId) {
        return pending.get(conversationId);
    }

    public void clear(String conversationId) {
        pending.remove(conversationId);
    }
}
