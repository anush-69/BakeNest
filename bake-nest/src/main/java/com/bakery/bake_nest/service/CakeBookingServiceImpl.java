package com.bakery.bake_nest.service;

import com.bakery.bake_nest.dto.CakeBookingRequest;
import com.bakery.bake_nest.dto.CakeBookingPageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.bakery.bake_nest.dto.CakeBookingResponse;
import com.bakery.bake_nest.model.CakeBooking;
import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.CakeBookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CakeBookingServiceImpl implements CakeBookingService {

    private final CakeBookingRepository bookingRepository;

    // Spring injects the repository through this constructor.
    public CakeBookingServiceImpl(
            CakeBookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public CakeBookingResponse createBooking(
            CakeBookingRequest request,
            User customer) {

        // Check that the request contains the required date.
        if (request == null || request.getRequiredDate() == null) {
            throw new IllegalArgumentException(
                    "A booking request with a required date must be provided.");
        }

        // The caller must supply a resolved existing database user.
        if (customer == null || customer.getId() == null) {
            throw new IllegalArgumentException(
                    "An existing customer must be provided.");
        }

        // Convert the request into a booking entity.
        CakeBooking booking = toEntity(request);

        // Assign the customer who owns this booking.
        booking.setCustomer(customer);

        // Save the entity through the repository.
        CakeBooking savedBooking = bookingRepository.save(booking);

        // Convert the saved entity into the response DTO.
        return toResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public CakeBookingResponse getBooking(Long bookingId, User customer) {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException(
                    "A valid booking ID must be provided.");
        }

        // The caller must supply a trusted existing database user.
        if (customer == null || customer.getId() == null) {
            throw new IllegalArgumentException(
                    "An existing customer must be provided.");
        }

        CakeBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found for this customer."));

        // Only return bookings owned by this customer.
        if (!customer.getId().equals(booking.getCustomer().getId())) {
            throw new IllegalArgumentException(
                    "Booking not found for this customer.");
        }

        return toResponse(booking);
    }

    @Override
    @Transactional
    public CakeBookingResponse updateBooking(
            Long bookingId, CakeBookingRequest request, User customer) {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException(
                    "A valid booking ID must be provided.");
        }
        // The caller must supply a trusted existing database user.
        if (customer == null || customer.getId() == null) {
            throw new IllegalArgumentException(
                    "An existing customer must be provided.");
        }
        if (request == null || request.getRequiredDate() == null) {
            throw new IllegalArgumentException(
                    "A booking request with a required date must be provided.");
        }

        CakeBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found for this customer."));
        if (!customer.getId().equals(booking.getCustomer().getId())) {
            throw new IllegalArgumentException(
                    "Booking not found for this customer.");
        }
        if (!"REQUESTED".equals(booking.getStatus())) {
            throw new IllegalStateException(
                    "Only requested bookings can be updated.");
        }

        // Replace editable fields, including optional null values.
        booking.setCakeType(request.getCakeType());
        booking.setCakeSize(request.getCakeSize());
        booking.setFlavour(request.getFlavour());
        booking.setRequiredDate(request.getRequiredDate());
        booking.setDesignDescription(request.getDesignDescription());
        booking.setCakeMessage(request.getCakeMessage());
        booking.setReferenceImageUrl(request.getReferenceImageUrl());

        CakeBooking savedBooking = bookingRepository.save(booking);
        return toResponse(savedBooking);
    }

    @Override
    @Transactional
    public CakeBookingResponse cancelBooking(Long bookingId, User customer) {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException(
                    "A valid booking ID must be provided.");
        }
        // The caller must supply a trusted existing database user.
        if (customer == null || customer.getId() == null) {
            throw new IllegalArgumentException(
                    "An existing customer must be provided.");
        }

        CakeBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found for this customer."));
        if (!customer.getId().equals(booking.getCustomer().getId())) {
            throw new IllegalArgumentException(
                    "Booking not found for this customer.");
        }
        if (!"REQUESTED".equals(booking.getStatus())) {
            throw new IllegalStateException(
                    "Only requested bookings can be cancelled.");
        }

        // Preserve the record and change only its status.
        booking.setStatus("CANCELLED");
        CakeBooking savedBooking = bookingRepository.save(booking);
        return toResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public CakeBookingPageResponse listBookings(User customer, int page, int size) {
        if (customer == null || customer.getId() == null) {
            throw new IllegalArgumentException("An existing customer must be provided.");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater.");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("Page size must be between 1 and 50.");
        }
        // Filter ownership in the database; the repository fixes newest-first ordering.
        Page<CakeBookingResponse> bookings = bookingRepository
                .findByCustomer_IdOrderByIdDesc(customer.getId(), PageRequest.of(page, size))
                .map(this::toResponse);
        return new CakeBookingPageResponse(bookings.getContent(), bookings.getNumber(),
                bookings.getSize(), bookings.getTotalElements(), bookings.getTotalPages());
    }

    // Converts request data into a new entity without saving it.
    private CakeBooking toEntity(CakeBookingRequest request) {
        CakeBooking booking = new CakeBooking();

        // Copy the customer-provided booking details.
        booking.setCakeType(request.getCakeType());
        booking.setCakeSize(request.getCakeSize());
        booking.setFlavour(request.getFlavour());
        booking.setRequiredDate(request.getRequiredDate());
        booking.setDesignDescription(request.getDesignDescription());
        booking.setCakeMessage(request.getCakeMessage());
        booking.setReferenceImageUrl(request.getReferenceImageUrl());

        // The required customer must be assigned before saving.
        return booking;
    }

    // Converts an entity into the data returned to the client.
    private CakeBookingResponse toResponse(CakeBooking booking) {
        CakeBookingResponse response = new CakeBookingResponse();

        response.setId(booking.getId());
        response.setCakeType(booking.getCakeType());
        response.setCakeSize(booking.getCakeSize());
        response.setFlavour(booking.getFlavour());
        response.setRequiredDate(booking.getRequiredDate());
        response.setDesignDescription(booking.getDesignDescription());
        response.setCakeMessage(booking.getCakeMessage());
        response.setReferenceImageUrl(booking.getReferenceImageUrl());
        response.setStatus(booking.getStatus());
        response.setEstimatedPrice(booking.getEstimatedPrice());
        response.setQuotedPrice(booking.getQuotedPrice());

        return response;
    }
}
