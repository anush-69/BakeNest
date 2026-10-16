package com.bakery.bake_nest;

import com.bakery.bake_nest.model.*;
import com.bakery.bake_nest.repository.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

// H2 keeps automated tests separate from the student's MySQL database.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:guestorders;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
class BakeNestApplicationTests {
    @LocalServerPort int port;
    @Autowired OrderRepository orders;
    @Autowired ProductRepository products;
    @Autowired EntityManager entityManager;
    @Autowired TransactionTemplate transactions;
    private Product product;

    @BeforeEach
    void addTestProduct() {
        transactions.executeWithoutResult(status -> {
            Category category = new Category();
            category.setName("Test category " + UUID.randomUUID());
            entityManager.persist(category);
            product = new Product();
            product.setName("Test Cupcake");
            product.setPrice(new BigDecimal("250.00"));
            product.setCategory(category);
            products.saveAndFlush(product);
        });
    }

    private HttpClient browser() {
        return HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(HttpClient client, Map<String, String> fields) throws Exception {
        String body = fields.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(java.util.stream.Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/orders"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, String> checkout(HttpClient client) throws Exception {
        HttpResponse<String> page = get(client, "/orders/new");
        assertEquals(200, page.statusCode()); // No login is needed.
        assertTrue(page.body().contains("Delivery details"));
        Map<String, String> fields = new LinkedHashMap<>();
        Matcher tags = Pattern.compile("<input [^>]*>").matcher(page.body());
        while (tags.find()) {
            Map<String, String> attributes = new HashMap<>();
            Matcher attr = Pattern.compile("([a-zA-Z0-9_-]+)=" + (char) 34 + "([^" + (char) 34 + "]*)" + (char) 34).matcher(tags.group());
            while (attr.find()) attributes.put(attr.group(1), attr.group(2));
            String name = attributes.get("name");
            if (name != null) fields.put(name, attributes.getOrDefault("value", ""));
        }
        assertTrue(fields.containsKey("_csrf"));
        for (var field : new ArrayList<>(fields.entrySet())) {
            if (field.getKey().endsWith(".productId")) {
                fields.put(field.getKey().replace(".productId", ".quantity"),
                        field.getValue().equals(product.getId().toString()) ? "2" : "0");
            }
        }
        fields.put("customerName", "Guest Student");
        fields.put("phone", "077 123 4567");
        fields.put("deliveryAddress", "12 Bakery Road, Colombo");
        fields.put("paymentMethod", "CASH_ON_DELIVERY");
        fields.put("customerNote", "Test " + UUID.randomUUID());
        return fields;
    }

    @Test
    void guestOrderSavesDeliveryDetailsItemsAndDatabasePrice() throws Exception {
        HttpClient client = browser();
        Map<String, String> fields = checkout(client);
        fields.put("status", "COMPLETED");
        fields.put("total", "0.01");
        long before = orders.count();
        assertEquals(302, post(client, fields).statusCode());
        assertEquals(before + 1, orders.count());
        String confirmation = get(client, "/orders/created").body();
        assertTrue(confirmation.contains("500.00"));
        assertTrue(confirmation.contains("PENDING"));
        transactions.executeWithoutResult(status -> {
            BakeryOrder order = orders.findAll().stream()
                    .filter(o -> fields.get("customerNote").equals(o.getCustomerNote())).findFirst().orElseThrow();
            assertNull(order.getCustomer());
            assertEquals("Guest Student", order.getCustomerName());
            assertEquals("077 123 4567", order.getPhone());
            assertEquals("12 Bakery Road, Colombo", order.getDeliveryAddress());
            assertEquals("PENDING", order.getStatus());
            assertEquals("CASH_ON_DELIVERY", order.getPaymentMethod());
            assertEquals("PENDING", order.getPaymentStatus());
            assertEquals(new BigDecimal("500.00"), order.getTotal());
            assertEquals(1, order.getItems().size());
        });
        get(client, "/orders/created");
        assertEquals(before + 1, orders.count());
        assertEquals(302, get(browser(), "/orders/created").statusCode());
    }

    @Test
    void cardDemoNeverMarksOrderPaidOrBindsCardDetails() throws Exception {
        HttpClient client = browser();
        Map<String, String> fields = checkout(client);
        fields.put("paymentMethod", "CARD_DEMO");
        fields.put("cardNumber", "4111111111111111");
        fields.put("cvv", "123");
        assertEquals(302, post(client, fields).statusCode());
        assertTrue(get(client, "/orders/created").body().contains("no payment has been processed"));
        transactions.executeWithoutResult(status -> {
            BakeryOrder order = orders.findAll().stream()
                    .filter(o -> fields.get("customerNote").equals(o.getCustomerNote())).findFirst().orElseThrow();
            assertEquals("NOT_PROCESSED", order.getPaymentStatus());
        });
    }

    @Test
    void rejectsInvalidDeliveryPaymentAndQuantities() throws Exception {
        HttpClient client = browser();
        Map<String, String> fields = checkout(client);
        long before = orders.count();
        fields.put("customerName", " ");
        assertEquals(200, post(client, fields).statusCode());
        fields.put("customerName", "Guest");
        fields.put("paymentMethod", "PAID");
        assertEquals(200, post(client, fields).statusCode());
        fields.put("paymentMethod", "CASH_ON_DELIVERY");
        fields.replaceAll((key, value) -> key.endsWith(".quantity") ? "0" : value);
        assertTrue(post(client, fields).body().contains("Choose at least one product"));
        fields.replaceAll((key, value) -> key.endsWith(".quantity") ? "-1" : value);
        assertEquals(200, post(client, fields).statusCode());
        fields.replaceAll((key, value) -> key.endsWith(".quantity") ? "1.5" : value);
        assertEquals(200, post(client, fields).statusCode());
        assertEquals(before, orders.count());
    }

    @Test
    void rejectsUnavailableProductsAndMissingCsrf() throws Exception {
        HttpClient client = browser();
        Map<String, String> fields = checkout(client);
        long before = orders.count();
        product.setAvailable(false);
        products.saveAndFlush(product);
        assertTrue(post(client, fields).body().contains("is unavailable"));
        fields.remove("_csrf");
        assertEquals(403, post(client, fields).statusCode());
        assertEquals(before, orders.count());
    }

    @Test
    void guestCanReadOnlyOrdersPlacedInTheSameBrowser() throws Exception {
        HttpClient guest = browser();
        HttpClient otherGuest = browser();
        String emptyList = get(guest, "/orders").body();
        assertTrue(emptyList.contains("No orders in this browser session yet"));

        Map<String, String> first = checkout(guest);
        assertEquals(302, post(guest, first).statusCode());
        Long firstId = orders.findAll().stream()
                .filter(o -> first.get("customerNote").equals(o.getCustomerNote()))
                .findFirst().orElseThrow().getId();
        get(guest, "/orders/created");

        Map<String, String> second = checkout(guest);
        assertEquals(302, post(guest, second).statusCode());
        Long secondId = orders.findAll().stream()
                .filter(o -> second.get("customerNote").equals(o.getCustomerNote()))
                .findFirst().orElseThrow().getId();

        String list = get(guest, "/orders").body();
        assertTrue(list.contains("/orders/" + firstId));
        assertTrue(list.indexOf("/orders/" + secondId) < list.indexOf("/orders/" + firstId));

        HttpResponse<String> details = get(guest, "/orders/" + firstId);
        assertEquals(200, details.statusCode());
        assertTrue(details.body().contains("Guest Student"));
        assertTrue(details.body().contains("12 Bakery Road, Colombo"));
        assertTrue(details.body().contains("Test Cupcake"));
        assertTrue(details.body().contains("500.00"));
        assertTrue(details.body().contains("Update Order (coming soon)"));
        assertTrue(details.body().contains("Delete Order (coming soon)"));

        assertEquals(404, get(otherGuest, "/orders/" + firstId).statusCode());
        assertTrue(get(otherGuest, "/orders").body().contains("No orders in this browser session yet"));
        assertEquals(404, get(guest, "/orders/99999999").statusCode());
    }
}
