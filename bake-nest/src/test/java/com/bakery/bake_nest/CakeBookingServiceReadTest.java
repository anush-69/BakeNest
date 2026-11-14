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

class CakeBookingServiceReadTest {

    private CakeBookingRepository bookingRepository;
    private CakeBookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(CakeBookingRepository.class);
        bookingService = new CakeBookingServiceImpl(bookingRepository);
    }

    @AfterEach
    void verifyNoOtherRepositoryCalls() {
        // Only explicitly verified reads are allowed, including no saves or deletes.
        verifyNoMoreInteractions(bookingRepository);
    }

    @Test
    void ownerReadsAllMappedBookingFields() {
        User customer = customerWithId(1000L);
        User owner = customerWithId(1000L);
        // Stub the generated ID without changing the entity.
        CakeBooking booking = spy(new CakeBooking());
        doReturn(42L).when(booking).getId();
        booking.setCustomer(owner);
        booking.setCakeType("Birthday");
        booking.setCakeSize("2kg");
        booking.setFlavour("Vanilla");
        booking.setRequiredDate(LocalDate.of(2030, 6, 15));
        booking.setDesignDescription("Test star design");
        booking.setCakeMessage("Test celebration");
        booking.setReferenceImageUrl("https://example.invalid/test-cake.png");
        booking.setStatus("QUOTED");
        booking.setEstimatedPrice(new BigDecimal("4500.00"));
        booking.setQuotedPrice(new BigDecimal("4750.00"));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));

        CakeBookingResponse response = bookingService.getBooking(42L, customer);

        assertNotNull(response);
        assertAll(
                () -> assertEquals(42L, response.getId()),
                () -> assertEquals("Birthday", response.getCakeType()),
                () -> assertEquals("2kg", response.getCakeSize()),
                () -> assertEquals("Vanilla", response.getFlavour()),
                () -> assertEquals(LocalDate.of(2030, 6, 15), response.getRequiredDate()),
                () -> assertEquals("Test star design", response.getDesignDescription()),
                () -> assertEquals("Test celebration", response.getCakeMessage()),
                () -> assertEquals("https://example.invalid/test-cake.png",
                        response.getReferenceImageUrl()),
                () -> assertEquals("QUOTED", response.getStatus()),
                () -> assertEquals(new BigDecimal("4500.00"), response.getEstimatedPrice()),
                () -> assertEquals(new BigDecimal("4750.00"), response.getQuotedPrice()));
        verify(bookingRepository).findById(42L);
    }

    @Test
    void missingBookingIsRejected() {
        User customer = customerWithId(1000L);
        when(bookingRepository.findById(42L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.getBooking(42L, customer));

        assertEquals("Booking not found for this customer.", exception.getMessage());
        verify(bookingRepository).findById(42L);
    }

    @Test
    void anotherCustomerIsRejected() {
        User customer = customerWithId(1000L);
        CakeBooking booking = new CakeBooking();
        booking.setCustomer(customerWithId(2000L));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.getBooking(42L, customer));

        assertEquals("Booking not found for this customer.", exception.getMessage());
        verify(bookingRepository).findById(42L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void invalidBookingIdIsRejectedWithoutRepositoryAccess(Long bookingId) {
        User customer = customerWithId(1000L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.getBooking(bookingId, customer));

        assertEquals("A valid booking ID must be provided.", exception.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void nullCustomerIsRejectedWithoutRepositoryAccess() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.getBooking(42L, null));

        assertEquals("An existing customer must be provided.", exception.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void customerWithoutIdIsRejectedWithoutRepositoryAccess() {
        User customer = new User();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.getBooking(42L, customer));

        assertEquals("An existing customer must be provided.", exception.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    private User customerWithId(Long id) {
        User customer = mock(User.class);
        when(customer.getId()).thenReturn(id);
        return customer;
    }
}
