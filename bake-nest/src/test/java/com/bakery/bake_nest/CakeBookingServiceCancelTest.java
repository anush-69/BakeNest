package com.bakery.bake_nest;

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

class CakeBookingServiceCancelTest {
    private CakeBookingRepository repository;
    private CakeBookingServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(CakeBookingRepository.class);
        service = new CakeBookingServiceImpl(repository);
    }

    @AfterEach
    void noUnexpectedRepositoryCalls() {
        // Only verified reads and saves are allowed; every delete call fails.
        verifyNoMoreInteractions(repository);
    }

    @Test
    void ownerCancelsExistingBookingAndPreservesEveryOtherField() {
        User owner = customer(1000L);
        User handler = new User();
        CakeBooking booking = existingBooking(owner);
        booking.setHandledBy(handler);
        when(repository.findById(42L)).thenReturn(Optional.of(booking));
        when(repository.save(same(booking))).thenAnswer(invocation -> {
            assertEquals("CANCELLED", booking.getStatus());
            return booking;
        });

        CakeBookingResponse response = service.cancelBooking(42L, customer(1000L));

        assertAll(
                () -> assertEquals("CANCELLED", booking.getStatus()),
                () -> assertEquals(42L, booking.getId()),
                () -> assertSame(owner, booking.getCustomer()),
                () -> assertEquals("Old type", booking.getCakeType()),
                () -> assertEquals("Old size", booking.getCakeSize()),
                () -> assertEquals("Old flavour", booking.getFlavour()),
                () -> assertEquals(LocalDate.of(2030, 1, 1), booking.getRequiredDate()),
                () -> assertEquals("Old design", booking.getDesignDescription()),
                () -> assertEquals("Old message", booking.getCakeMessage()),
                () -> assertEquals("https://example.invalid/old.png", booking.getReferenceImageUrl()),
                () -> assertEquals(new BigDecimal("100.00"), booking.getEstimatedPrice()),
                () -> assertEquals(new BigDecimal("125.00"), booking.getQuotedPrice()),
                () -> assertSame(handler, booking.getHandledBy()),
                () -> assertEquals("Server note", booking.getAdminNote()));
        assertResponseMatches(booking, response);
        verify(repository).findById(42L);
        verify(repository).save(same(booking));
    }

    @Test
    void responseMapsEntityReturnedBySave() {
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
        saved.setStatus("CANCELLED");
        saved.setEstimatedPrice(new BigDecimal("200.00"));
        saved.setQuotedPrice(new BigDecimal("250.00"));
        when(repository.findById(42L)).thenReturn(Optional.of(booking));
        when(repository.save(same(booking))).thenReturn(saved);

        CakeBookingResponse response = service.cancelBooking(42L, customer(1000L));

        assertResponseMatches(saved, response);
        verify(repository).findById(42L);
        verify(repository).save(same(booking));
    }

    @Test
    void repeatedCancellationIsRejectedWithoutAnotherSave() {
        User owner = customer(1000L);
        CakeBooking booking = existingBooking(owner);
        when(repository.findById(42L)).thenReturn(Optional.of(booking));
        when(repository.save(same(booking))).thenReturn(booking);
        service.cancelBooking(42L, owner);
        verify(repository).findById(42L);
        verify(repository).save(same(booking));
        clearInvocations(repository);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.cancelBooking(42L, owner));

        assertEquals("Only requested bookings can be cancelled.", error.getMessage());
        assertEquals("CANCELLED", booking.getStatus());
        verify(repository).findById(42L);
    }

    @Test
    void missingBookingIsRejected() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.cancelBooking(42L, customer(1000L)));
        assertEquals("Booking not found for this customer.", error.getMessage());
        verify(repository).findById(42L);
    }

    @Test
    void wrongCustomerIsRejectedBeforeStatusCheck() {
        CakeBooking booking = existingBooking(customer(2000L));
        booking.setStatus("CONFIRMED");
        when(repository.findById(42L)).thenReturn(Optional.of(booking));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.cancelBooking(42L, customer(1000L)));
        assertEquals("Booking not found for this customer.", error.getMessage());
        assertEquals("CONFIRMED", booking.getStatus());
        verify(repository).findById(42L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"CONFIRMED", "CANCELLED", "QUOTED", "requested", ""})
    void otherStatusesAreRejectedWithoutSaving(String status) {
        CakeBooking booking = existingBooking(customer(1000L));
        booking.setStatus(status);
        when(repository.findById(42L)).thenReturn(Optional.of(booking));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.cancelBooking(42L, customer(1000L)));
        assertEquals("Only requested bookings can be cancelled.", error.getMessage());
        assertEquals(status, booking.getStatus());
        verify(repository).findById(42L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void invalidIdsAreRejectedBeforeCustomerValidation(Long id) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.cancelBooking(id, null));
        assertEquals("A valid booking ID must be provided.", error.getMessage());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidCustomersAreRejectedBeforeRepositoryAccess(boolean missingCustomer) {
        User invalidCustomer = missingCustomer ? null : new User();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.cancelBooking(42L, invalidCustomer));
        assertEquals("An existing customer must be provided.", error.getMessage());
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
