package com.bakery.bake_nest;

import com.bakery.bake_nest.controller.CakeBookingController;
import com.bakery.bake_nest.controller.CakeBookingExceptionHandler;
import com.bakery.bake_nest.dto.CakeBookingPageResponse;
import com.bakery.bake_nest.dto.CakeBookingResponse;
import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.security.CurrentCustomerResolver;
import com.bakery.bake_nest.service.CakeBookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CakeBookingListControllerTest {
    private CakeBookingService service;
    private CurrentCustomerResolver resolver;
    private User customer;
    private Authentication authentication;
    private MockMvc mvc;
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        service = mock(CakeBookingService.class);
        resolver = mock(CurrentCustomerResolver.class);
        customer = new User();
        authentication = UsernamePasswordAuthenticationToken.authenticated("test", null, List.of());
        when(resolver.resolve(authentication)).thenReturn(customer);
        mvc = MockMvcBuilders.standaloneSetup(new CakeBookingController(service, resolver))
                .setControllerAdvice(new CakeBookingExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void listsOnlyResolvedCustomerWithDefaultOrExplicitPagination(boolean explicit) throws Exception {
        int page = explicit ? 2 : 0;
        int size = explicit ? 5 : 10;
        CakeBookingResponse item = new CakeBookingResponse();
        item.setId(42L);
        item.setStatus("CANCELLED");
        CakeBookingPageResponse response = new CakeBookingPageResponse(List.of(item), page, size, 21L,
                explicit ? 5 : 3);
        when(service.listBookings(customer, page, size)).thenReturn(response);
        String query = explicit ? "?page=2&size=5&customerId=999&sort=id,asc" : "";
        MvcResult result = mvc.perform(get("/api/cake-bookings" + query).principal(authentication))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertEquals(5, body.size());
        assertEquals(json.readTree(json.writeValueAsString(response)), body);
        assertEquals("CANCELLED", body.get("items").get(0).get("status").asString());
        verify(resolver).resolve(same(authentication));
        verify(service).listBookings(same(customer), eq(page), eq(size));
        verifyNoMoreInteractions(resolver, service);
    }

    @ParameterizedTest
    @CsvSource({"-1,10,Page must be zero or greater.",
            "0,0,Page size must be between 1 and 50.",
            "0,51,Page size must be between 1 and 50."})
    void paginationValidationReturnsOnlyPublicErrorFields(int page, int size, String message) throws Exception {
        when(service.listBookings(customer, page, size)).thenThrow(new IllegalArgumentException(message));
        MvcResult result = mvc.perform(get("/api/cake-bookings").param("page", String.valueOf(page))
                .param("size", String.valueOf(size)).principal(authentication)).andReturn();
        assertError(result, 400, message);
        verify(resolver).resolve(same(authentication));
        verify(service).listBookings(same(customer), eq(page), eq(size));
        verifyNoMoreInteractions(resolver, service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"page", "size"})
    void rejectsPaginationConversionBeforeService(String parameter) throws Exception {
        MvcResult result = mvc.perform(get("/api/cake-bookings").param(parameter, "invalid")
                .principal(authentication)).andReturn();
        assertError(result, 400, "Invalid booking request.");
        verifyNoInteractions(service, resolver);
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void resolverRejectionPreventsListing(int status) throws Exception {
        RuntimeException error = status == 401
                ? new AuthenticationCredentialsNotFoundException("Private detail")
                : new AccessDeniedException("Private detail");
        when(resolver.resolve(authentication)).thenThrow(error);
        MvcResult result = mvc.perform(get("/api/cake-bookings").principal(authentication)).andReturn();
        assertError(result, status, status == 401 ? "Authentication required." : "Access denied.");
        verify(resolver).resolve(same(authentication));
        verifyNoMoreInteractions(resolver);
        verifyNoInteractions(service);
    }

    private void assertError(MvcResult result, int status, String message) throws Exception {
        assertEquals(status, result.getResponse().getStatus());
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertEquals(2, body.size());
        assertEquals(status, body.get("status").asInt());
        assertEquals(message, body.get("message").asString());
    }
}
