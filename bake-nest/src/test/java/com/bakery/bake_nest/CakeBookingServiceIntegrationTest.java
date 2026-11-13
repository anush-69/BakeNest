package com.bakery.bake_nest;

import com.bakery.bake_nest.dto.CakeBookingRequest;
import com.bakery.bake_nest.dto.CakeBookingResponse;
import com.bakery.bake_nest.model.CakeBooking;
import com.bakery.bake_nest.model.User;
import com.bakery.bake_nest.repository.CakeBookingRepository;
import com.bakery.bake_nest.service.CakeBookingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.sql.init.mode=never"
        }
)
@Transactional
@Rollback
class CakeBookingServiceIntegrationTest {

    @Autowired
    private CakeBookingService bookingService;

    @Autowired
    private CakeBookingRepository bookingRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long createdBookingId;
    private Long createdCustomerId;

    @BeforeTransaction
    void verifyFixtureTablesAreSafeForRollback() {
        // Check storage and triggers before the transaction can write the fixture.
        Integer transactionalTables = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.TABLES t
                JOIN information_schema.ENGINES e ON e.ENGINE = t.ENGINE
                WHERE t.TABLE_SCHEMA = DATABASE()
                  AND t.TABLE_NAME IN (?, ?) AND e.TRANSACTIONS = 'YES'
                """, Integer.class, "users", "cake_bookings");
        assertEquals(2, transactionalTables,
                "Both fixture tables must use transactional storage engines.");
        Integer triggers = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.TRIGGERS
                WHERE TRIGGER_SCHEMA = DATABASE()
                  AND EVENT_OBJECT_TABLE IN (?, ?)
                """, Integer.class, "users", "cake_bookings");
        assertEquals(0, triggers,
                "Fixture tables must have no triggers before fixture writes.");
    }

    @Test
    void createBookingShouldSaveAndReturnBookingDetails() {

        // Create a unique temporary customer in the test-managed transaction.
        String fixtureSuffix = UUID.randomUUID().toString().replace("-", "");
        User customer = new User();
        customer.setUsername("it_" + fixtureSuffix);
        customer.setEmail("it_" + fixtureSuffix + "@example.invalid");
        customer.setPasswordHash(new BCryptPasswordEncoder().encode(
                UUID.randomUUID().toString()));
        entityManager.persist(customer);
        createdCustomerId = customer.getId();
        assertNotNull(createdCustomerId);
        Long customerId = createdCustomerId;

        // Prepare a sample booking request.
        CakeBookingRequest request = new CakeBookingRequest();
        request.setCakeType("Birthday");
        request.setCakeSize("1kg");
        request.setFlavour("Chocolate");
        request.setRequiredDate(LocalDate.now().plusDays(7));
        request.setDesignDescription("Integration test design");
        request.setCakeMessage("Test booking");
        request.setReferenceImageUrl(null);

        // Call the real service and repository.
        CakeBookingResponse response =
                bookingService.createBooking(request, customer);

        assertNotNull(response);
        assertNotNull(response.getId());
        createdBookingId = response.getId();

        // Send pending changes to the database and clear cached entities.
        entityManager.flush();
        entityManager.clear();

        // Read the booking again from the database.
        CakeBooking storedBooking = entityManager.find(
                CakeBooking.class, createdBookingId);

        assertNotNull(storedBooking);
        assertEquals(customerId, storedBooking.getCustomer().getId());
        assertEquals("Birthday", storedBooking.getCakeType());
        assertEquals(request.getRequiredDate(),
                storedBooking.getRequiredDate());
        assertEquals("REQUESTED", storedBooking.getStatus());
        assertEquals(storedBooking.getFlavour(), response.getFlavour());
        assertEquals(storedBooking.getStatus(), response.getStatus());
    }

    @AfterTransaction
    void testFixtureShouldNotRemainAfterRollback() {
        // Run both checks even if one fails; missing IDs must not silently skip checks.
        assertAll("Both temporary records must be rolled back",
                () -> {
                    assertNotNull(createdBookingId, "The booking fixture must have been saved.");
                    assertFalse(bookingRepository.existsById(createdBookingId),
                            "The test booking must not remain after rollback.");
                },
                () -> {
                    assertNotNull(createdCustomerId, "The user fixture must have been saved.");
                    assertEquals(0L, jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM users WHERE id = ?",
                            Long.class, createdCustomerId),
                            "The test user must not remain after rollback.");
                });
    }
}
