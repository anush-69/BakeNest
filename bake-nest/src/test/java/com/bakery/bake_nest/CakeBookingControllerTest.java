package com.bakery.bake_nest;

import com.bakery.bake_nest.controller.CakeBookingController;
import com.bakery.bake_nest.controller.CakeBookingExceptionHandler;
import com.bakery.bake_nest.dto.CakeBookingRequest;
import com.bakery.bake_nest.dto.CakeBookingResponse;
import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.security.CurrentCustomerResolver;
import com.bakery.bake_nest.service.CakeBookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CakeBookingControllerTest {
    private static final String BASE = "/api/cake-bookings";
    private static final String BODY = """
            {"cakeType":"Birthday","cakeSize":"2kg","flavour":"Vanilla",
             "requiredDate":"2030-06-15","designDescription":"Star design",
             "cakeMessage":"Test celebration","referenceImageUrl":"https://example.invalid/cake.png"}
            """;
    private final JsonMapper json = JsonMapper.builder().build();
    private CakeBookingService service;
    private CurrentCustomerResolver resolver;
    private User customer;
    private Authentication authentication;
    private CakeBookingResponse response;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(CakeBookingService.class);
        resolver = mock(CurrentCustomerResolver.class);
        customer = new User();
        authentication = UsernamePasswordAuthenticationToken.authenticated("test-principal", null, List.of());
        when(resolver.resolve(authentication)).thenReturn(customer);
        response = new CakeBookingResponse();
        response.setId(42L);
        response.setCakeType("Birthday");
        response.setCakeSize("2kg");
        response.setFlavour("Vanilla");
        response.setRequiredDate(LocalDate.of(2030, 6, 15));
        response.setDesignDescription("Star design");
        response.setCakeMessage("Test celebration");
        response.setReferenceImageUrl("https://example.invalid/cake.png");
        response.setStatus("REQUESTED");
        response.setEstimatedPrice(new BigDecimal("100.00"));
        response.setQuotedPrice(new BigDecimal("125.00"));
        // Standalone MVC has no application context or security filter chain.
        mvc = MockMvcBuilders.standaloneSetup(new CakeBookingController(service, resolver))
                .setControllerAdvice(new CakeBookingExceptionHandler()).build();
    }

    @AfterEach
    void noUnexpectedCalls() {
        verifyNoMoreInteractions(service, resolver);
    }

    @Test
    void postCreatesBookingWithLocationAndTrustedCustomer() throws Exception {
        when(service.createBooking(any(), same(customer))).thenReturn(response);
        MvcResult result = mvc.perform(route("POST", "42", BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", BASE + "/42")).andReturn();
        assertResponse(result);
        ArgumentCaptor<CakeBookingRequest> request = ArgumentCaptor.forClass(CakeBookingRequest.class);
        verify(service).createBooking(request.capture(), same(customer));
        assertEditableFields(request.getValue(), false);
        verify(resolver).resolve(same(authentication));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void putPassesAllEditableFieldsIncludingNulls(boolean clearOptional) throws Exception {
        String body = clearOptional ? "{\"requiredDate\":\"2030-06-15\"}" : BODY;
        when(service.updateBooking(eq(42L), any(), same(customer))).thenReturn(response);
        MvcResult result = mvc.perform(route("PUT", "42", body))
                .andExpect(status().isOk()).andReturn();
        assertResponse(result);
        ArgumentCaptor<CakeBookingRequest> request = ArgumentCaptor.forClass(CakeBookingRequest.class);
        verify(service).updateBooking(eq(42L), request.capture(), same(customer));
        assertEditableFields(request.getValue(), clearOptional);
        verify(resolver).resolve(same(authentication));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PATCH"})
    void readAndCancelDelegateWithTrustedCustomer(String method) throws Exception {
        if (method.equals("GET")) {
            when(service.getBooking(42L, customer)).thenReturn(response);
        } else {
            response.setStatus("CANCELLED");
            when(service.cancelBooking(42L, customer)).thenReturn(response);
        }
        MvcResult result = mvc.perform(route(method, "42", null))
                .andExpect(status().isOk()).andReturn();
        assertResponse(result);
        if (method.equals("GET")) {
            verify(service).getBooking(eq(42L), same(customer));
        } else {
            verify(service).cancelBooking(eq(42L), same(customer));
        }
        verify(resolver).resolve(same(authentication));
    }

    @Test
    void jsonCustomerIdAndHeaderCannotReplaceResolvedCustomer() throws Exception {
        when(service.createBooking(any(), same(customer))).thenReturn(response);
        String body = BODY.strip().replaceFirst("\\{", "{\"customerId\":999,");
        mvc.perform(route("POST", "42", body).header("customerId", "999"))
                .andExpect(status().isCreated());
        ArgumentCaptor<CakeBookingRequest> request = ArgumentCaptor.forClass(CakeBookingRequest.class);
        verify(service).createBooking(request.capture(), same(customer));
        assertEditableFields(request.getValue(), false);
        verify(resolver).resolve(same(authentication));
    }

    @ParameterizedTest
    @CsvSource({"POST,401", "GET,401", "PUT,401", "PATCH,401",
            "POST,403", "GET,403", "PUT,403", "PATCH,403"})
    void resolverRejectionPreventsServiceInvocation(String method, int status) throws Exception {
        RuntimeException failure = status == 401
                ? new AuthenticationCredentialsNotFoundException("Private authentication detail")
                : new AccessDeniedException("Private access detail");
        when(resolver.resolve(authentication)).thenThrow(failure);
        MvcResult result = mvc.perform(route(method, "42", BODY)).andReturn();
        assertError(result, status, status == 401 ? "Authentication required." : "Access denied.");
        verify(resolver).resolve(same(authentication));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "PATCH"})
    void absentOrOtherOwnerBookingReturnsGeneric404(String method) throws Exception {
        stubFailure(method, new IllegalArgumentException("Booking not found for this customer."));
        assertError(mvc.perform(route(method, "42", BODY)).andReturn(),
                404, "Booking not found for this customer.");
        verifyCall(method, 42L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"A valid booking ID must be provided.",
            "An existing customer must be provided.",
            "A booking request with a required date must be provided."})
    void knownValidationMessagesReturn400(String message) throws Exception {
        when(service.createBooking(any(), same(customer))).thenThrow(new IllegalArgumentException(message));
        assertError(mvc.perform(route("POST", "42", "{}")).andReturn(), 400, message);
        verifyCall("POST", 42L);
    }

    @ParameterizedTest
    @CsvSource({"GET,0", "GET,-1", "PUT,0", "PUT,-1", "PATCH,0", "PATCH,-1"})
    void invalidNumericIdReachesServiceValidation(String method, long id) throws Exception {
        String message = "A valid booking ID must be provided.";
        if (method.equals("GET")) {
            when(service.getBooking(eq(id), same(customer))).thenThrow(new IllegalArgumentException(message));
        } else if (method.equals("PUT")) {
            when(service.updateBooking(eq(id), any(), same(customer))).thenThrow(new IllegalArgumentException(message));
        } else {
            when(service.cancelBooking(eq(id), same(customer))).thenThrow(new IllegalArgumentException(message));
        }
        assertError(mvc.perform(route(method, Long.toString(id), BODY)).andReturn(), 400, message);
        verifyCall(method, id);
    }

    @ParameterizedTest
    @CsvSource({"POST,syntax", "PUT,syntax", "POST,date", "PUT,date",
            "POST,missing", "PUT,missing", "POST,null", "PUT,null"})
    void malformedBodiesReturnGeneric400BeforeService(String method, String kind) throws Exception {
        String body = switch (kind) {
            case "syntax" -> "{broken";
            case "date" -> "{\"requiredDate\":\"not-a-date\"}";
            case "null" -> "null";
            default -> "";
        };
        assertError(mvc.perform(route(method, "42", body)).andReturn(), 400, "Invalid booking request.");
        verifyNoInteractions(service, resolver);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "PATCH"})
    void invalidIdConversionReturnsGeneric400(String method) throws Exception {
        assertError(mvc.perform(route(method, "not-a-number", BODY)).andReturn(),
                400, "Invalid booking request.");
        verifyNoInteractions(service, resolver);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "PATCH"})
    void updateAndCancellationConflictsReturn409(String method) throws Exception {
        String message = method.equals("PUT") ? "Only requested bookings can be updated."
                : "Only requested bookings can be cancelled.";
        stubFailure(method, new IllegalStateException(message));
        assertError(mvc.perform(route(method, "42", BODY)).andReturn(), 409, message);
        verifyCall(method, 42L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void arbitraryServiceExceptionMessagesAreNotExposed(boolean stateException) throws Exception {
        RuntimeException failure = stateException ? new IllegalStateException("Private internal detail")
                : new IllegalArgumentException("Private internal detail");
        stubFailure("GET", failure);
        assertError(mvc.perform(route("GET", "42", null)).andReturn(),
                500, "Unable to process booking request.");
        verifyCall("GET", 42L);
    }

    @Test
    void controllerAndAdviceAreProfileScopedAndAdviceTargetsOnlyBookingController() {
        assertNull(CakeBookingController.class.getAnnotation(Profile.class));
        assertNull(CakeBookingExceptionHandler.class.getAnnotation(Profile.class));
        assertArrayEquals(new Class<?>[]{CakeBookingController.class}, CakeBookingExceptionHandler.class
                .getAnnotation(org.springframework.web.bind.annotation.RestControllerAdvice.class).assignableTypes());
    }

    private MockHttpServletRequestBuilder route(String method, String id, String body) {
        MockHttpServletRequestBuilder request = switch (method) {
            case "POST" -> post(BASE);
            case "PUT" -> put(BASE + "/" + id);
            case "PATCH" -> patch(BASE + "/" + id + "/cancel");
            default -> get(BASE + "/" + id);
        };
        request.principal(authentication);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return request;
    }

    private void stubFailure(String method, RuntimeException failure) {
        switch (method) {
            case "PUT" -> when(service.updateBooking(eq(42L), any(), same(customer))).thenThrow(failure);
            case "PATCH" -> when(service.cancelBooking(eq(42L), same(customer))).thenThrow(failure);
            default -> when(service.getBooking(eq(42L), same(customer))).thenThrow(failure);
        }
    }

    private void verifyCall(String method, long id) {
        switch (method) {
            case "POST" -> verify(service).createBooking(any(CakeBookingRequest.class), same(customer));
            case "PUT" -> verify(service).updateBooking(eq(id), any(CakeBookingRequest.class), same(customer));
            case "PATCH" -> verify(service).cancelBooking(eq(id), same(customer));
            default -> verify(service).getBooking(eq(id), same(customer));
        }
        verify(resolver).resolve(same(authentication));
    }

    private void assertResponse(MvcResult result) throws Exception {
        assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(result.getResponse().getContentType())));
        JsonNode actual = json.readTree(result.getResponse().getContentAsString());
        assertEquals(11, actual.size());
        assertEquals(json.readTree(json.writeValueAsString(response)), actual);
    }

    private void assertError(MvcResult result, int status, String message) throws Exception {
        assertEquals(status, result.getResponse().getStatus());
        assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(result.getResponse().getContentType())));
        JsonNode error = json.readTree(result.getResponse().getContentAsString());
        assertEquals(2, error.size());
        assertEquals(status, error.get("status").asInt());
        assertEquals(message, error.get("message").asString());
    }

    private void assertEditableFields(CakeBookingRequest request, boolean cleared) {
        assertAll(
                () -> assertEquals(cleared ? null : "Birthday", request.getCakeType()),
                () -> assertEquals(cleared ? null : "2kg", request.getCakeSize()),
                () -> assertEquals(cleared ? null : "Vanilla", request.getFlavour()),
                () -> assertEquals(LocalDate.of(2030, 6, 15), request.getRequiredDate()),
                () -> assertEquals(cleared ? null : "Star design", request.getDesignDescription()),
                () -> assertEquals(cleared ? null : "Test celebration", request.getCakeMessage()),
                () -> assertEquals(cleared ? null : "https://example.invalid/cake.png", request.getReferenceImageUrl()));
    }
}
