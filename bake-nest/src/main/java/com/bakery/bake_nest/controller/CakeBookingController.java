package com.bakery.bake_nest.controller;

import com.bakery.bake_nest.dto.CakeBookingRequest;
import com.bakery.bake_nest.dto.CakeBookingPageResponse;
import com.bakery.bake_nest.dto.CakeBookingResponse;
import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.security.CurrentCustomerResolver;
import com.bakery.bake_nest.service.CakeBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/cake-bookings")
public class CakeBookingController {
    private final CakeBookingService bookingService;
    private final CurrentCustomerResolver customerResolver;

    public CakeBookingController(CakeBookingService bookingService,
            CurrentCustomerResolver customerResolver) {
        this.bookingService = bookingService;
        this.customerResolver = customerResolver;
    }

    @GetMapping
    public CakeBookingPageResponse listBookings(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Authentication authentication) {
        User customer = customerResolver.resolve(authentication);
        return bookingService.listBookings(customer, page, size);
    }

    @PostMapping
    public ResponseEntity<CakeBookingResponse> createBooking(
            @RequestBody CakeBookingRequest request, Authentication authentication) {
        User customer = customerResolver.resolve(authentication);
        CakeBookingResponse response = bookingService.createBooking(request, customer);
        return ResponseEntity.created(URI.create("/api/cake-bookings/" + response.getId()))
                .body(response);
    }

    @GetMapping("/{bookingId}")
    public CakeBookingResponse getBooking(@PathVariable("bookingId") Long bookingId,
            Authentication authentication) {
        User customer = customerResolver.resolve(authentication);
        return bookingService.getBooking(bookingId, customer);
    }

    @PutMapping("/{bookingId}")
    public CakeBookingResponse updateBooking(@PathVariable("bookingId") Long bookingId,
            @RequestBody CakeBookingRequest request, Authentication authentication) {
        User customer = customerResolver.resolve(authentication);
        return bookingService.updateBooking(bookingId, request, customer);
    }

    @PatchMapping("/{bookingId}/cancel")
    public CakeBookingResponse cancelBooking(@PathVariable("bookingId") Long bookingId,
            Authentication authentication) {
        User customer = customerResolver.resolve(authentication);
        return bookingService.cancelBooking(bookingId, customer);
    }
}
