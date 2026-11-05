package com.bakery.bake_nest;

import com.bakery.bake_nest.dto.CakeBookingRequest;
import com.bakery.bake_nest.dto.CakeBookingResponse;
import com.bakery.bake_nest.model.CakeBooking;
import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.CakeBookingRepository;
import com.bakery.bake_nest.service.CakeBookingServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CakeBookingServiceUpdateTest {
    private CakeBookingRepository repository;
    private CakeBookingServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(CakeBookingRepository.class);
        service = new CakeBookingServiceImpl(repository);
    }

    @AfterEach
    void noUnexpectedRepositoryCalls() {
        // Unverified saves, deletes and other calls fail every test.
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ownerReplacesEditableFieldsAndPreservesServerFields(boolean clearOptionalFields) {
        User owner = customer(1000L);
        User handler = new User();
        CakeBooking booking = existingBooking(owner);
        booking.setHandledBy(handler);
        CakeBookingRequest request = validRequest();
        if (clearOptionalFields) {
            request.setCakeType(null);
            request.setCakeSize(null);
            request.setFlavour(null);
            request.setDesignDescription(null);
            request.setCakeMessage(null);
            request.setReferenceImageUrl(null);
        }
        when(repository.findById(42L)).thenReturn(Optional.of(booking));
        when(repository.save(same(booking))).thenReturn(booking);

        CakeBookingResponse response = service.updateBooking(42L, request, customer(1000L));

        assertAll(
                () -> assertEquals(request.getCakeType(), booking.getCakeType()),
                () -> assertEquals(request.getCakeSize(), booking.getCakeSize()),
                () -> assertEquals(request.getFlavour(), booking.getFlavour()),
                () -> assertEquals(request.getRequiredDate(), booking.getRequiredDate()),
                () -> assertEquals(request.getDesignDescription(), booking.getDesignDescription()),
                () -> assertEquals(request.getCakeMessage(), booking.getCakeMessage()),
                () -> assertEquals(request.getReferenceImageUrl(), booking.getReferenceImageUrl()),
                () -> assertEquals(42L, booking.getId()),
                () -> assertSame(owner, booking.getCustomer()),
                () -> assertEquals("REQUESTED", booking.getStatus()),
                () -> assertEquals(new BigDecimal("100.00"), booking.getEstimatedPrice()),
                () -> assertEquals(new BigDecimal("125.00"), booking.getQuotedPrice()),
                () -> assertSame(handler, booking.getHandledBy()),
                () -> assertEquals("Server note", booking.getAdminNote()));
        assertResponseMatches(booking, response);
        verify(repository).findById(42L);
        verify(repository).save(same(booking));
    }

    @Test
    void responseUsesEntityReturnedBySave() {
        CakeBooking booking = existingBooking(customer(1000L));
        CakeBooking saved = existingBooking(customer(1000L));
        doReturn(99L).when(saved).getId();
        saved.setCakeType("Returned type");
        saved.setCakeSize("Returned size");
        saved.setFlavour("Returned flavour");
        saved.setRequiredDate(LocalDate.of(2031, 7, 20));
        saved.setDesignDescription("Returned design");
        saved.setCakeMessage("Returned message");
        saved.setReferenceImageUrl("https://example.invalid/returned.png");
        saved.setStatus("QUOTED");
        saved.setEstimatedPrice(new BigDecimal("200.00"));
        saved.setQuotedPrice(new BigDecimal("250.00"));
        when(repository.findById(42L)).thenReturn(Optional.of(booking));
        when(repository.save(same(booking))).thenReturn(saved);

        CakeBookingResponse response = service.updateBooking(42L, validRequest(), customer(1000L));

        assertResponseMatches(saved, response);
        verify(repository).findById(42L);
        verify(repository).save(same(booking));
    }

    @Test
    void missingBookingIsRejected() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.updateBooking(42L, validRequest(), customer(1000L)));
        assertEquals("Booking not found for this customer.", error.getMessage());
        verify(repository).findById(42L);
    }

    @Test
    void differentCustomerIsRejectedBeforeStatusCheck() {
        CakeBooking booking = existingBooking(customer(2000L));
        booking.setStatus("QUOTED");
        when(repository.findById(42L)).thenReturn(Optional.of(booking));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.updateBooking(42L, validRequest(), customer(1000L)));
        assertEquals("Booking not found for this customer.", error.getMessage());
        assertEquals("Old type", booking.getCakeType());
        verify(repository).findById(42L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"QUOTED", "CONFIRMED", "CANCELLED", "requested", ""})
    void otherStatusesAreRejected(String status) {
        CakeBooking booking = existingBooking(customer(1000L));
        booking.setStatus(status);
        when(repository.findById(42L)).thenReturn(Optional.of(booking));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.updateBooking(42L, validRequest(), customer(1000L)));
        assertEquals("Only requested bookings can be updated.", error.getMessage());
        assertEquals("Old type", booking.getCakeType());
        verify(repository).findById(42L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void invalidIdIsRejectedFirst(Long id) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.updateBooking(id, null, null));
        assertEquals("A valid booking ID must be provided.", error.getMessage());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidCustomerIsRejectedBeforeRequest(boolean missingCustomer) {
        User invalidCustomer = missingCustomer ? null : new User();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.updateBooking(42L, null, invalidCustomer));
        assertEquals("An existing customer must be provided.", error.getMessage());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidRequestIsRejectedBeforeRepositoryAccess(boolean missingRequest) {
        CakeBookingRequest invalidRequest = missingRequest ? null : new CakeBookingRequest();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.updateBooking(42L, invalidRequest, customer(1000L)));
        assertEquals("A booking request with a required date must be provided.", error.getMessage());
        verifyNoInteractions(repository);
    }

    private User customer(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private CakeBooking existingBooking(User owner) {
        // Stub generated IDs without reflection or production setters.
        CakeBooking booking = spy(new CakeBooking());
        doReturn(42L).when(booking).getId();
        booking.setCustomer(owner);
        booking.setCakeType("Old type");
        booking.setCakeSize("Old size");
        booking.setFlavour("Old flavour");
        booking.setRequiredDate(LocalDate.of(2030, 1, 1));
        booking.setDesignDescription("Old design");
        booking.setCakeMessage("Old message");
        booking.setReferenceImageUrl("https://example.invalid/old.png");
        booking.setEstimatedPrice(new BigDecimal("100.00"));
        booking.setQuotedPrice(new BigDecimal("125.00"));
        booking.setAdminNote("Server note");
        return booking;
    }

    private CakeBookingRequest validRequest() {
        CakeBookingRequest request = new CakeBookingRequest();
        request.setCakeType("Birthday");
        request.setCakeSize("2kg");
        request.setFlavour("Vanilla");
        request.setRequiredDate(LocalDate.of(2030, 6, 15));
        request.setDesignDescription("New design");
        request.setCakeMessage("New message");
        request.setReferenceImageUrl("https://example.invalid/new.png");
        return request;
    }

    private void assertResponseMatches(CakeBooking entity, CakeBookingResponse response) {
        assertNotNull(response);
        assertAll(
                () -> assertEquals(entity.getId(), response.getId()),
                () -> assertEquals(entity.getCakeType(), response.getCakeType()),
                () -> assertEquals(entity.getCakeSize(), response.getCakeSize()),
                () -> assertEquals(entity.getFlavour(), response.getFlavour()),
                () -> assertEquals(entity.getRequiredDate(), response.getRequiredDate()),
                () -> assertEquals(entity.getDesignDescription(), response.getDesignDescription()),
                () -> assertEquals(entity.getCakeMessage(), response.getCakeMessage()),
                () -> assertEquals(entity.getReferenceImageUrl(), response.getReferenceImageUrl()),
                () -> assertEquals(entity.getStatus(), response.getStatus()),
                () -> assertEquals(entity.getEstimatedPrice(), response.getEstimatedPrice()),
                () -> assertEquals(entity.getQuotedPrice(), response.getQuotedPrice()));
    }
}
