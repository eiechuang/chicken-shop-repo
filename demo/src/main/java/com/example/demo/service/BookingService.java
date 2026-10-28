package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.BookingDTO;
import com.example.demo.repo.BookingRepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final Logger logger =
        LoggerFactory.getLogger(BookingService.class);

    private final BookingRepo bookingRepo;

    private int bookingCounter = 0;

    public BookingService(BookingRepo bookingRepo) {
        this.bookingRepo = bookingRepo;
    }

    @Transactional
    public void createBooking(
            Long userId,
            Long slotId,
            String instructions) {

        bookingRepo.createBooking(
            userId,
            slotId,
            instructions
        );

        bookingCounter++;

        logger.info(
            "Booking created: userId={}, slotId={}",
            userId,
            slotId
        );
    }

    public List<BookingDTO> getBookingsForUser(
            Long userId) {

        return bookingRepo.findByUserId(userId);
    }

    public List<BookingDTO> getAllBookings() {

        return bookingRepo.findAllBookings();
    }

    @Transactional
    public boolean cancelBooking(
            Long orderId,
            Long userId) {

        int rowsUpdated =
            bookingRepo.cancelBooking(
                orderId,
                userId
            );

        if (rowsUpdated > 0) {

            logger.info(
                "Booking cancelled: orderId={}, userId={}",
                orderId,
                userId
            );

            return true;
        }

        logger.warn(
            "Cancellation failed: orderId={}, userId={}",
            orderId,
            userId
        );

        return false;
    }

    public int getBookingCount() {
        return bookingCounter;
    }
}