package com.bakery.bake_nest;

import com.bakery.bake_nest.dto.CakeBookingPageResponse;
import com.bakery.bake_nest.dto.CakeBookingResponse;
import com.bakery.bake_nest.model.CakeBooking;
import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.CakeBookingRepository;
import com.bakery.bake_nest.service.CakeBookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CakeBookingServiceListTest {
    private CakeBookingRepository repository;
    private CakeBookingServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(CakeBookingRepository.class);
        service = new CakeBookingServiceImpl(repository);
    }

    @Test
    void filtersOwnerInRepositoryAndMapsAllResponseFieldsAndPageMetadata() {
        User customer = customer(1000L);
        CakeBooking booking = spy(new CakeBooking());
        doReturn(42L).when(booking).getId();
        booking.setCakeType("Birthday");
        booking.setCakeSize("2kg");
        booking.setFlavour("Vanilla");
        booking.setRequiredDate(LocalDate.of(2030, 6, 15));
        booking.setDesignDescription("Test design");
        booking.setCakeMessage("Test message");
        booking.setReferenceImageUrl("https://example.invalid/test.png");
        booking.setStatus("CANCELLED");
        booking.setEstimatedPrice(new BigDecimal("100.00"));
        booking.setQuotedPrice(new BigDecimal("125.00"));
        CakeBooking newer = spy(new CakeBooking());
        doReturn(43L).when(newer).getId();
        PageRequest requested = PageRequest.of(1, 10);
        when(repository.findByCustomer_IdOrderByIdDesc(1000L, requested))
                .thenReturn(new PageImpl<>(List.of(newer, booking), requested, 25L));

        CakeBookingPageResponse page = service.listBookings(customer, 1, 10);

        assertEquals(1, page.page());
        assertEquals(10, page.size());
        assertEquals(25L, page.totalElements());
        assertEquals(3, page.totalPages());
        assertEquals(List.of(43L, 42L), page.items().stream().map(CakeBookingResponse::getId).toList());
        CakeBookingResponse response = page.items().get(1);
        assertAll(
                () -> assertEquals(42L, response.getId()),
                () -> assertEquals("Birthday", response.getCakeType()),
                () -> assertEquals("2kg", response.getCakeSize()),
                () -> assertEquals("Vanilla", response.getFlavour()),
                () -> assertEquals(booking.getRequiredDate(), response.getRequiredDate()),
                () -> assertEquals("Test design", response.getDesignDescription()),
                () -> assertEquals("Test message", response.getCakeMessage()),
                () -> assertEquals(booking.getReferenceImageUrl(), response.getReferenceImageUrl()),
                () -> assertEquals("CANCELLED", response.getStatus()),
                () -> assertEquals(new BigDecimal("100.00"), response.getEstimatedPrice()),
                () -> assertEquals(new BigDecimal("125.00"), response.getQuotedPrice()));
        verify(repository).findByCustomer_IdOrderByIdDesc(1000L, requested);
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({"0,1", "0,50", "2,10"})
    void acceptsPaginationBoundariesAndReturnsEmptyPage(int page, int size) {
        User customer = customer(2000L);
        PageRequest requested = PageRequest.of(page, size);
        when(repository.findByCustomer_IdOrderByIdDesc(2000L, requested))
                .thenReturn(new PageImpl<>(List.of(), requested, 0L));
        CakeBookingPageResponse response = service.listBookings(customer, page, size);
        assertTrue(response.items().isEmpty());
        assertEquals(page, response.page());
        assertEquals(size, response.size());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        verify(repository).findByCustomer_IdOrderByIdDesc(2000L, requested);
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({"-1,10,Page must be zero or greater.",
            "0,0,Page size must be between 1 and 50.",
            "0,-1,Page size must be between 1 and 50.",
            "0,51,Page size must be between 1 and 50."})
    void rejectsInvalidPaginationBeforeRepositoryAccess(int page, int size, String message) {
        User customer = customer(1000L);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.listBookings(customer, page, size));
        assertEquals(message, error.getMessage());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsInvalidCustomerBeforePaginationAndRepository(boolean missing) {
        User customer = missing ? null : new User();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.listBookings(customer, -1, 0));
        assertEquals("An existing customer must be provided.", error.getMessage());
        verifyNoInteractions(repository);
    }

    private User customer(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}
