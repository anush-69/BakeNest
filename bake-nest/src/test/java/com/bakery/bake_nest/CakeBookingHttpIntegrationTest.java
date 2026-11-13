package com.bakery.bake_nest;

import com.bakery.bake_nest.dto.CakeBookingRequest;
import com.bakery.bake_nest.model.CakeBooking;
import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.security.DatabaseUserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false",
        "debug=false",
        "trace=false",
        "logging.level.root=WARN",
        "spring.mvc.log-request-details=false",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF",
        "spring.jpa.properties.hibernate.cache.use_second_level_cache=false",
        "logging.level.org.hibernate=OFF",
        "logging.level.org.springframework.security=OFF",
        "logging.level.org.springframework.web=OFF",
        "logging.level.org.springframework.jdbc=OFF",
        "logging.level.com.zaxxer.hikari=OFF",
        "logging.level.org.springframework.boot.autoconfigure.security=OFF"
})
@ActiveProfiles("booking-local")
@Transactional
@Rollback
class CakeBookingHttpIntegrationTest {
    private static final String BASE = "/api/cake-bookings";
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy filterChain;
    @Autowired private JdbcTemplate jdbc;
    @PersistenceContext private EntityManager entityManager;
    private final JsonMapper json = JsonMapper.builder().build();
    private final String ownerName = "http_owner_" + UUID.randomUUID().toString().replace("-", "");
    private final String otherName = "http_other_" + UUID.randomUUID().toString().replace("-", "");
    private Long ownerId;
    private Long otherId;
    private Long bookingId;
    private MockMvc mvc;

    @BeforeTransaction
    void verifyStorageBeforeFixtureWrites() {
        assertEquals(2, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.TABLES t
                JOIN information_schema.ENGINES e ON e.ENGINE = t.ENGINE
                WHERE t.TABLE_SCHEMA = DATABASE() AND t.TABLE_NAME IN (?, ?)
                  AND e.TRANSACTIONS = 'YES'
                """, Integer.class, "users", "cake_bookings"),
                "Both fixture tables must exist and support transactions.");
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.TRIGGERS
                WHERE TRIGGER_SCHEMA = DATABASE() AND EVENT_OBJECT_TABLE IN (?, ?)
                """, Integer.class, "users", "cake_bookings"),
                "Fixture tables must have no triggers.");
        passed("Transactional tables and absence of triggers");
    }

    @BeforeEach
    void buildMvcWithRealFilters() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filterChain).build();
    }

    @Test
    void authenticatedBookingWorkflowRollsBackAllFixtures() throws Exception {
        mvc.perform(get(BASE + "/1"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/cake-bookings/live"))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        passed("Anonymous request redirected to login by security filters");

        String ownerPassword = UUID.randomUUID().toString();
        String otherPassword = UUID.randomUUID().toString();
        User owner = persistUser(ownerName, ownerPassword);
        ownerId = owner.getId();
        User other = persistUser(otherName, otherPassword);
        otherId = other.getId();
        assertNotNull(ownerId);
        assertNotNull(otherId);
        entityManager.flush();
        entityManager.clear();

        MockHttpSession ownerSession = login(ownerName, ownerPassword, ownerId);
        passed("Owner real login, authenticated session and fresh CSRF token");
        String[] renderedCsrf = assertLivePage("/cake-bookings/live", ownerSession, "list");
        assertLivePage("/cake-bookings/live/new", ownerSession, "create");
        mvc.perform(get("/js/cake-bookings-live.js").session(ownerSession)).andExpect(status().isOk());
        passed("Authenticated list/create pages render with security metadata and live script is served");
        CakeBookingRequest original = request(false);
        // Use the same rendered header metadata that the customer JavaScript reads.
        MvcResult created = mvc.perform(post(BASE).session(ownerSession)
                        .header(renderedCsrf[0], renderedCsrf[1])
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(original)))
                .andExpect(status().isCreated()).andReturn();
        JsonNode createdBody = json.readTree(created.getResponse().getContentAsString());
        bookingId = createdBody.get("id").asLong();
        assertTrue(bookingId > 0);
        assertEquals(BASE + "/" + bookingId, created.getResponse().getHeader("Location"));
        assertResponse(created, original, "REQUESTED");
        assertStored(original, "REQUESTED");
        passed("Create response and database readback with owner relationship");
        assertListing(ownerSession, true, "REQUESTED");
        MvcResult nextPage = mvc.perform(get(BASE).session(ownerSession).param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andReturn();
        JsonNode nextPageBody = json.readTree(nextPage.getResponse().getContentAsString());
        assertEquals(0, nextPageBody.get("items").size());
        assertEquals(1L, nextPageBody.get("totalElements").asLong());
        assertEquals(1, nextPageBody.get("page").asInt());
        assertLivePage("/cake-bookings/live/" + bookingId, ownerSession, "details");
        assertLivePage("/cake-bookings/live/" + bookingId + "/edit", ownerSession, "edit");
        passed("Owner-filtered listing, pagination, details/edit pages and rendered CSRF header accepted");

        MvcResult read = mvc.perform(get(BASE + "/" + bookingId).session(ownerSession))
                .andExpect(status().isOk()).andReturn();
        assertResponse(read, original, "REQUESTED");
        passed("Owner HTTP read");

        CakeBookingRequest updated = request(true);
        String updatedJson = json.writeValueAsString(updated);
        MvcResult update = mvc.perform(withCsrf(put(BASE + "/" + bookingId).session(ownerSession), ownerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(updatedJson))
                .andExpect(status().isOk()).andReturn();
        assertResponse(update, updated, "REQUESTED");
        assertStored(updated, "REQUESTED");
        passed("Update response and database readback including optional null clearing");

        MockHttpSession otherSession = login(otherName, otherPassword, otherId);
        passed("Other customer real login and independent session");
        assertListing(otherSession, false, null);
        passed("Other customer listing excludes the owner's booking");
        assertNotFound(mvc.perform(get(BASE + "/" + bookingId).session(otherSession)).andReturn());
        assertNotFound(mvc.perform(withCsrf(put(BASE + "/" + bookingId).session(otherSession), otherSession)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(original))).andReturn());
        assertNotFound(mvc.perform(withCsrf(patch(BASE + "/" + bookingId + "/cancel")
                .session(otherSession), otherSession)).andReturn());
        assertStored(updated, "REQUESTED");
        assertFixtureBookingCount(1L);
        passed("Other-customer GET, PUT and cancellation rejected with unchanged database row");

        for (String method : new String[]{"POST", "PUT", "PATCH"}) {
            for (boolean wrongSessionToken : new boolean[]{false, true}) {
                MockHttpServletRequestBuilder mutation = mutation(method).session(ownerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(original));
                if (wrongSessionToken) {
                    // A genuine token from the other session must not authorize this session.
                    CsrfToken token = csrf(otherSession);
                    mutation.param(token.getParameterName(), token.getToken());
                }
                mvc.perform(mutation).andExpect(status().isForbidden());
                assertFixtureBookingCount(1L);
                assertStored(updated, "REQUESTED");
            }
        }
        passed("Missing and cross-session CSRF tokens reject POST, PUT and PATCH without writes");

        MvcResult cancelled = mvc.perform(withCsrf(patch(BASE + "/" + bookingId + "/cancel")
                        .session(ownerSession), ownerSession))
                .andExpect(status().isOk()).andReturn();
        assertResponse(cancelled, updated, "CANCELLED");
        assertStored(updated, "CANCELLED");
        assertFixtureBookingCount(1L);
        passed("Cancellation response and database readback preserve the same row and owner");
        assertListing(ownerSession, true, "CANCELLED");
        passed("Cancelled booking remains in owner listing");

        assertConflict(mvc.perform(withCsrf(put(BASE + "/" + bookingId).session(ownerSession), ownerSession)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(original))).andReturn(),
                "Only requested bookings can be updated.");
        assertConflict(mvc.perform(withCsrf(patch(BASE + "/" + bookingId + "/cancel")
                .session(ownerSession), ownerSession)).andReturn(),
                "Only requested bookings can be cancelled.");
        assertStored(updated, "CANCELLED");
        assertFixtureBookingCount(1L);
        passed("Updating and recancelling cancelled booking return 409 without changes");
    }

    private void assertListing(MockHttpSession session, boolean ownsBooking, String expectedStatus) throws Exception {
        MvcResult result = mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode page = json.readTree(result.getResponse().getContentAsString());
        assertEquals(5, page.size());
        assertEquals(0, page.get("page").asInt());
        assertEquals(10, page.get("size").asInt());
        assertEquals(ownsBooking ? 1L : 0L, page.get("totalElements").asLong());
        assertEquals(ownsBooking ? 1 : 0, page.get("totalPages").asInt());
        assertEquals(ownsBooking ? 1 : 0, page.get("items").size());
        if (ownsBooking) {
            assertEquals(bookingId.longValue(), page.get("items").get(0).get("id").asLong());
            assertEquals(expectedStatus, page.get("items").get(0).get("status").asString());
        }
    }

    private String[] assertLivePage(String path, MockHttpSession session, String mode) throws Exception {
        MvcResult page = mvc.perform(get(path).session(session)).andExpect(status().isOk()).andReturn();
        String html = page.getResponse().getContentAsString();
        // Boolean checks keep rendered tokens out of assertion failure output.
        assertTrue(html.contains("data-live-page=\"" + mode + "\""), "Expected live page must render.");
        assertTrue(html.contains("/js/cake-bookings-live.js"), "Live script must be linked.");
        assertFalse(html.contains("/js/cake-bookings.js"), "Prototype script must remain separate.");
        assertTrue(html.contains("action=\"/logout\""), "Logout form must be rendered.");
        assertTrue(Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"[^\"]+\"")
                .matcher(html).find(), "Logout form must contain a genuine CSRF field.");
        String token = metaValue(html, "_csrf");
        String header = metaValue(html, "_csrf_header");
        assertTrue(header.equals(tokenFrom(page).getHeaderName()), "Rendered CSRF header must match the filter.");
        assertTrue(token.equals(tokenFrom(page).getToken()), "Rendered CSRF value must match the filter.");
        return new String[]{header, token};
    }

    private String metaValue(String html, String name) {
        var matcher = Pattern.compile("<meta[^>]*name=\"" + Pattern.quote(name)
                + "\"[^>]*content=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Required security metadata must render.");
        assertTrue(!matcher.group(1).isBlank(), "Security metadata must not be empty.");
        return matcher.group(1);
    }

    private User persistUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.invalid");
        user.setStatus("ACTIVE");
        user.setPasswordHash(new BCryptPasswordEncoder().encode(password));
        entityManager.persist(user);
        return user;
    }

    private MockHttpSession login(String username, String password, Long expectedId) throws Exception {
        MvcResult page = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        CsrfToken token = tokenFrom(page);
        MockHttpSession beforeLogin = (MockHttpSession) page.getRequest().getSession(false);
        assertTrue(beforeLogin != null, "Login page must establish a CSRF session.");
        MvcResult login = mvc.perform(post("/login").session(beforeLogin)
                        .param("username", username).param("password", password)
                        .param(token.getParameterName(), token.getToken()))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/")).andReturn();
        // Take the resulting session because authentication can rotate or replace it.
        MockHttpSession authenticatedSession = (MockHttpSession) login.getRequest().getSession(false);
        assertTrue(authenticatedSession != null, "Successful login must retain a session.");
        Object savedContext = authenticatedSession.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertTrue(savedContext instanceof SecurityContext, "Login must save the security context.");
        SecurityContext security = (SecurityContext) savedContext;
        assertTrue(security.getAuthentication().isAuthenticated());
        assertTrue(security.getAuthentication().getPrincipal() instanceof DatabaseUserPrincipal);
        DatabaseUserPrincipal principal = (DatabaseUserPrincipal) security.getAuthentication().getPrincipal();
        assertEquals(expectedId, principal.getUserId());
        assertTrue(principal.getPassword() == null, "Authenticated credentials must be erased.");
        csrf(authenticatedSession);
        return authenticatedSession;
    }

    private CsrfToken csrf(MockHttpSession session) throws Exception {
        MvcResult page = mvc.perform(get("/login").session(session))
                .andExpect(status().isOk()).andReturn();
        return tokenFrom(page);
    }

    private CsrfToken tokenFrom(MvcResult result) {
        Object attribute = result.getRequest().getAttribute(CsrfToken.class.getName());
        assertTrue(attribute instanceof CsrfToken, "Real CSRF filter must expose a token.");
        CsrfToken token = (CsrfToken) attribute;
        assertTrue(token.getToken() != null && !token.getToken().isBlank(), "CSRF token must be available.");
        return token;
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request,
            MockHttpSession session) throws Exception {
        CsrfToken token = csrf(session);
        return request.param(token.getParameterName(), token.getToken());
    }

    private MockHttpServletRequestBuilder mutation(String method) {
        return switch (method) {
            case "POST" -> post(BASE);
            case "PUT" -> put(BASE + "/" + bookingId);
            default -> patch(BASE + "/" + bookingId + "/cancel");
        };
    }

    private CakeBookingRequest request(boolean updated) {
        CakeBookingRequest request = new CakeBookingRequest();
        request.setCakeType(updated ? "Celebration" : "Birthday");
        request.setCakeSize(updated ? "2kg" : "1kg");
        request.setFlavour(updated ? "Vanilla" : "Chocolate");
        request.setRequiredDate(LocalDate.now().plusDays(updated ? 14 : 7));
        request.setDesignDescription(updated ? null : "Temporary HTTP test design");
        request.setCakeMessage(updated ? null : "Temporary test message");
        request.setReferenceImageUrl(updated ? null : "https://example.invalid/test.png");
        return request;
    }

    private void assertResponse(MvcResult result, CakeBookingRequest expected, String status) throws Exception {
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertEquals(11, body.size());
        assertEquals(bookingId.longValue(), body.get("id").asLong());
        assertEquals(status, body.get("status").asString());
        JsonNode fields = json.readTree(json.writeValueAsString(expected));
        for (String field : new String[]{"cakeType", "cakeSize", "flavour", "requiredDate",
                "designDescription", "cakeMessage", "referenceImageUrl"}) {
            assertEquals(fields.get(field), body.get(field), field);
        }
        assertTrue(body.get("estimatedPrice").isNull());
        assertTrue(body.get("quotedPrice").isNull());
    }

    private void assertStored(CakeBookingRequest expected, String status) {
        entityManager.flush();
        entityManager.clear();
        CakeBooking stored = entityManager.find(CakeBooking.class, bookingId);
        assertNotNull(stored);
        assertAll(
                () -> assertEquals(bookingId, stored.getId()),
                () -> assertEquals(ownerId, stored.getCustomer().getId()),
                () -> assertEquals(status, stored.getStatus()),
                () -> assertEquals(expected.getCakeType(), stored.getCakeType()),
                () -> assertEquals(expected.getCakeSize(), stored.getCakeSize()),
                () -> assertEquals(expected.getFlavour(), stored.getFlavour()),
                () -> assertEquals(expected.getRequiredDate(), stored.getRequiredDate()),
                () -> assertEquals(expected.getDesignDescription(), stored.getDesignDescription()),
                () -> assertEquals(expected.getCakeMessage(), stored.getCakeMessage()),
                () -> assertEquals(expected.getReferenceImageUrl(), stored.getReferenceImageUrl()),
                () -> assertNull(stored.getEstimatedPrice()),
                () -> assertNull(stored.getQuotedPrice()),
                () -> assertNull(stored.getHandledBy()),
                () -> assertNull(stored.getAdminNote()));
    }

    private void assertFixtureBookingCount(long expected) {
        assertEquals(expected, jdbc.queryForObject(
                "SELECT COUNT(*) FROM cake_bookings WHERE customer_id IN (?, ?)",
                Long.class, ownerId, otherId));
    }

    private void assertNotFound(MvcResult result) throws Exception {
        assertEquals(404, result.getResponse().getStatus());
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertEquals(2, body.size());
        assertEquals(404, body.get("status").asInt());
        assertEquals("Booking not found for this customer.", body.get("message").asString());
    }

    private void assertConflict(MvcResult result, String message) throws Exception {
        assertEquals(409, result.getResponse().getStatus());
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertEquals(2, body.size());
        assertEquals(409, body.get("status").asInt());
        assertEquals(message, body.get("message").asString());
    }

    @AfterTransaction
    void verifyAllFixtureRecordsWereRolledBack() {
        // Name and owner fallbacks also cover failures before IDs are captured.
        assertAll("All temporary records must be absent after rollback",
                () -> {
                    assertEquals(0L, jdbc.queryForObject("""
                            SELECT COUNT(*) FROM cake_bookings
                            WHERE id = ? OR customer_id IN (?, ?)
                            """, Long.class, bookingId, ownerId, otherId));
                    passed("After rollback: temporary booking absent");
                },
                () -> {
                    assertEquals(0L, jdbc.queryForObject(
                            "SELECT COUNT(*) FROM users WHERE id = ? OR username = ?",
                            Long.class, ownerId, ownerName));
                    passed("After rollback: owner user absent");
                },
                () -> {
                    assertEquals(0L, jdbc.queryForObject(
                            "SELECT COUNT(*) FROM users WHERE id = ? OR username = ?",
                            Long.class, otherId, otherName));
                    passed("After rollback: other user absent");
                });
    }

    private void passed(String stage) {
        // Report milestones without credentials, session identifiers or tokens.
        System.out.println("BOOKING_HTTP_CHECK PASSED: " + stage);
    }
}
