package com.bakery.bake_nest.service;

import com.bakery.bake_nest.dto.CakeBookingRequest;
import com.bakery.bake_nest.dto.CakeBookingPageResponse;
import com.bakery.bake_nest.dto.CakeBookingResponse;
import com.bakery.bake_nest.model.User;

public interface CakeBookingService {

    // Declares the operation and its required parameters.
    CakeBookingResponse createBooking(
            CakeBookingRequest request,
            User customer);

    // The caller must supply a trusted existing database user.
    CakeBookingResponse getBooking(Long bookingId, User customer);

    CakeBookingResponse updateBooking(
            Long bookingId,
            CakeBookingRequest request,
            User customer);

    CakeBookingResponse cancelBooking(Long bookingId, User customer);

    CakeBookingPageResponse listBookings(User customer, int page, int size);
}
