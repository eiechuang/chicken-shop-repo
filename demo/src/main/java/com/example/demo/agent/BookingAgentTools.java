package com.example.demo.agent;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.demo.dto.BookingDTO;
import com.example.demo.dto.NotificationDTO;
import com.example.demo.dto.PickupSlotDTO;
import com.example.demo.service.BookingService;
import com.example.demo.service.NotificationGateway;
import com.example.demo.service.SlotService;

/**
 * Per-request tool object. It acts only as the logged-in customer supplied by
 * AgentController and delegates all writes to the existing transactional service.
 */
public class BookingAgentTools {

    private final Long userId;
    private final String username;
    private final String role;
    private final String conversationId;
    private final String turnId;
    private final String userMessage;
    private final SlotService slotService;
    private final BookingService bookingService;
    private final NotificationGateway notificationGateway;
    private final PendingActionStore pendingActions;

    public BookingAgentTools(
            Long userId,
            String username,
            String role,
            String conversationId,
            String turnId,
            String userMessage,
            SlotService slotService,
            BookingService bookingService,
            NotificationGateway notificationGateway,
            PendingActionStore pendingActions) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.conversationId = conversationId;
        this.turnId = turnId;
        this.userMessage = userMessage;
        this.slotService = slotService;
        this.bookingService = bookingService;
        this.notificationGateway = notificationGateway;
        this.pendingActions = pendingActions;
    }

    @Tool(description = "List pickup slots that the customer can choose from. Use this to check availability.")
    public List<PickupSlotDTO> checkAvailability() {
        requireCustomer();
        return slotService.getAvailableSlots();
    }

    @Tool(description = "List the logged-in customer's existing pickup bookings and their order IDs.")
    public List<BookingDTO> listMyBookings() {
        requireCustomer();
        return bookingService.getBookingsForUser(userId);
    }

    @Tool(description = "Prepare a booking for a slot. This does NOT write to the database. Call this first and present the proposed booking to the user for explicit confirmation.")
    public String prepareBooking(@ToolParam(description = "Pickup slot ID") Long slotId) {
        requireCustomer();
        PickupSlotDTO slot;
        try {
            slot = slotService.getSlotById(slotId);
        }
        catch (Exception ex) {
            return "Slot not found. Use checkAvailability and offer a valid slot.";
        }
        pendingActions.prepareBooking(conversationId, slotId, turnId);
        return "Prepared booking for slot " + slotId + " from " + slot.startTime()
            + " to " + slot.endTime() + ". Ask the user to confirm before booking.";
    }

    @Tool(description = "Commit the previously prepared booking only after the user explicitly confirms it on a later chat turn. Never call this on the same turn as prepareBooking.")
    public String confirmBooking(
            @ToolParam(description = "Pickup slot ID that was previously prepared") Long slotId,
            @ToolParam(description = "Optional pickup instructions", required = false) String instructions) {
        requireCustomer();
        PendingActionStore.PendingAction pending = pendingActions.get(conversationId);
        if (pending == null || !"BOOK".equals(pending.type()) || !slotId.equals(pending.targetId())) {
            return "No matching booking is awaiting confirmation. Prepare the booking first.";
        }
        if (turnId.equals(pending.preparedTurnId())) {
            return "Confirmation must come from the user on a later turn. Do not book yet.";
        }
        if (!isExplicitConfirmation()) {
            return "The user has not explicitly confirmed this booking on the current turn. Do not book yet.";
        }
        try {
            bookingService.createBooking(userId, slotId, instructions);
        }
        catch (DataIntegrityViolationException ex) {
            pendingActions.clear(conversationId);
            return "Booking was not created because this customer already has that slot. Offer another slot.";
        }
        pendingActions.clear(conversationId);
        notificationGateway.send(new NotificationDTO(
            java.util.UUID.randomUUID().toString(),
            username,
            "BOOKING_CONFIRMATION",
            "Your pickup booking was confirmed."
        ));
        return "Booking confirmed for slot " + slotId + ".";
    }

    @Tool(description = "Prepare cancellation of one of the logged-in customer's bookings. This does NOT write. Ask the user for confirmation after calling it.")
    public String prepareCancellation(@ToolParam(description = "Order ID belonging to the logged-in customer") Long orderId) {
        requireCustomer();
        boolean owned = bookingService.getBookingsForUser(userId).stream()
            .anyMatch(b -> orderId.equals(b.orderId()) && "BOOKED".equals(b.status()));
        if (!owned) {
            return "No active booking with that order ID belongs to this customer.";
        }
        pendingActions.prepareCancellation(conversationId, orderId, turnId);
        return "Prepared cancellation for order " + orderId + ". Ask the user to confirm before cancelling.";
    }

    @Tool(description = "Commit a previously prepared cancellation only after explicit user confirmation on a later turn.")
    public String confirmCancellation(@ToolParam(description = "Order ID that was previously prepared for cancellation") Long orderId) {
        requireCustomer();
        PendingActionStore.PendingAction pending = pendingActions.get(conversationId);
        if (pending == null || !"CANCEL".equals(pending.type()) || !orderId.equals(pending.targetId())) {
            return "No matching cancellation is awaiting confirmation. Prepare the cancellation first.";
        }
        if (turnId.equals(pending.preparedTurnId())) {
            return "Confirmation must come from the user on a later turn. Do not cancel yet.";
        }
        if (!isExplicitConfirmation()) {
            return "The user has not explicitly confirmed this cancellation on the current turn. Do not cancel yet.";
        }
        boolean cancelled = bookingService.cancelBooking(orderId, userId);
        if (!cancelled) {
            pendingActions.clear(conversationId);
            return "Cancellation failed because the booking is not active or does not belong to this customer.";
        }
        pendingActions.clear(conversationId);
        notificationGateway.send(new NotificationDTO(
            "CANCEL-" + orderId,
            username,
            "CANCELLATION",
            "Your pickup booking was cancelled."
        ));
        return "Order " + orderId + " was cancelled.";
    }

    private boolean isExplicitConfirmation() {
        if (userMessage == null) {
            return false;
        }
        String normalized = userMessage.trim().toLowerCase();
        return normalized.equals("yes")
            || normalized.equals("confirm")
            || normalized.equals("confirmed")
            || normalized.contains("yes, confirm")
            || normalized.contains("yes confirm")
            || normalized.contains("go ahead")
            || normalized.contains("do it")
            || normalized.contains("please book")
            || normalized.contains("please cancel");
    }

    private void requireCustomer() {
        if (userId == null || !"CUSTOMER".equals(role)) {
            throw new SecurityException("Agent tools may only act as the logged-in customer.");
        }
    }
}
