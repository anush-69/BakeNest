package com.bakery.bake_nest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bakery.bake_nest.model.*;
import com.bakery.bake_nest.order.repository.OrderRepository;
import com.bakery.bake_nest.product.repository.*;
import com.bakery.bake_nest.repository.*;
import com.bakery.bake_nest.review.repository.ReviewRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BackendIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RoleRepositorys roles;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired OrderRepository orders;
    @Autowired ReviewRepository reviews;
    @Autowired PasswordEncoder passwords;
    private User customer, other, admin;
    private Product product;
    private Category category;

    @BeforeEach
    void prepare() {
        Role customerRole = role("CUSTOMER"), adminRole = role("ADMIN");
        customer = account("customer", customerRole);
        other = account("other", customerRole);
        admin = account("admin", adminRole);
        category = new Category();
        category.setName("Bread");
        category = categories.save(category);
        product = new Product();
        product.setName("Milk bread");
        product.setCategory(category);
        product.setPrice(new BigDecimal("125.50"));
        product = products.saveAndFlush(product);
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return roles.save(role);
    }

    private User account(String name, Role role) {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@example.com");
        user.setFirstName(name);
        user.setLastName("Test");
        user.setPasswordHash(passwords.encode("Password123!"));
        user.getRoles().add(role);
        return users.saveAndFlush(user);
    }

    private String registration(String name) {
        return """
        {"username":"%s","email":"%s@example.com","password":"Password123!","firstName":"New","lastName":"User","phone":"0771234567"}
        """
                .formatted(name, name);
    }

    private long id(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    private long createOrder() throws Exception {
        return id(
                mvc.perform(
                                post("/api/orders")
                                        .with(user("customer"))
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"items\":[{\"productId\":"
                                                        + product.getId()
                                                        + ",\"quantity\":2}]}"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.total").value(251.00)));
    }

    private long createReview() throws Exception {
        return id(
                mvc.perform(
                                post("/api/reviews")
                                        .with(user("customer"))
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"productId\":"
                                                        + product.getId()
                                                        + ",\"rating\":5,\"comment\":\"Excellent\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.createdAt").isNotEmpty()));
    }

    @Test
    void registrationHashesPasswordAndNeverAcceptsAdminRole() throws Exception {
        String request = registration("newcustomer").trim();
        request =
                request.substring(0, request.length() - 1)
                        + ",\"roles\":[\"ADMIN\"],\"status\":\"ACTIVE\"}";
        mvc.perform(
                        post("/api/users/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(
                        passwords.matches(
                                "Password123!",
                                users.findByUsername("newcustomer")
                                        .orElseThrow()
                                        .getPasswordHash()))
                .isTrue();
        mvc.perform(
                        post("/api/users/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registration("NEWCUSTOMER")))
                .andExpect(status().isConflict());
    }

    @Test
    void csrfAndAuthenticationAreRequired() throws Exception {
        mvc.perform(
                        post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registration("newcustomer")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginFailureAndLogoutUseExistingFormFlow() throws Exception {
        mvc.perform(formLogin().user("customer").password("wrong"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
        var login =
                mvc.perform(formLogin().user("customer").password("Password123!"))
                        .andExpect(authenticated())
                        .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/users/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("customer"));
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void customerCannotReadOrChangeAnotherAccount() throws Exception {
        mvc.perform(get("/api/users/" + other.getId()).with(user("customer")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/users/" + other.getId()).with(user("customer")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/users/" + other.getId())
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"changed@example.com\",\"firstName\":\"A\",\"lastName\":\"B\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/users").with(user("customer"))).andExpect(status().isForbidden());
        mvc.perform(
                        get("/api/users/by-email")
                                .param("email", other.getEmail())
                                .with(user("customer")))
                .andExpect(status().isForbidden());
    }

    @Test
    void profileUpdateUniquenessAndAdminLookup() throws Exception {
        String profile =
                "{\"email\":\"newmail@example.com\",\"firstName\":\"New\",\"lastName\":\"Name\",\"phone\":\"0771112222\"}";
        mvc.perform(
                        put("/api/users/" + customer.getId())
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(profile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("New"));
        mvc.perform(
                        put("/api/users/" + customer.getId())
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(profile.replace("newmail", "other")))
                .andExpect(status().isConflict());
        mvc.perform(
                        get("/api/users/by-email")
                                .param("email", "newmail@example.com")
                                .with(user("admin")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/users/by-username").param("username", "CUSTOMER").with(user("admin")))
                .andExpect(status().isOk());
    }

    @Test
    void passwordChangeRequiresCurrentPasswordAndRevokesSessions() throws Exception {
        var login = mvc.perform(formLogin().user("customer").password("Password123!")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        String request = "{\"currentPassword\":\"wrong\",\"newPassword\":\"NewPassword123!\"}";
        mvc.perform(
                        put("/api/users/me/password")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put("/api/users/me/password")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request.replace("wrong", "Password123!")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/users/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(formLogin().user("customer").password("Password123!"))
                .andExpect(unauthenticated());
        mvc.perform(formLogin().user("customer").password("NewPassword123!"))
                .andExpect(authenticated());
    }

    @Test
    void adminCrudIsProtectedAndSelfDeletionBlocked() throws Exception {
        mvc.perform(
                        post("/api/admins")
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registration("secondadmin")))
                .andExpect(status().isForbidden());
        long id =
                id(
                        mvc.perform(
                                        post("/api/admins")
                                                .with(user("admin"))
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(registration("secondadmin")))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.roles[0]").value("ADMIN")));
        mvc.perform(get("/api/admins").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/admins/" + id).with(user("admin"))).andExpect(status().isOk());
        mvc.perform(
                        put("/api/admins/" + id)
                                .with(user("admin"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"secondadmin@example.com\",\"firstName\":\"Updated\",\"lastName\":\"Admin\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/admins/" + admin.getId()).with(user("admin")).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/admins/" + id).with(user("admin")).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(formLogin().user("secondadmin").password("Password123!"))
                .andExpect(unauthenticated());
    }

    @Test
    void roleCrudAndReservedRoleProtection() throws Exception {
        mvc.perform(get("/api/roles").with(user("customer"))).andExpect(status().isForbidden());
        long id =
                id(
                        mvc.perform(
                                        post("/api/roles")
                                                .with(user("admin"))
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"name\":\"staff\"}"))
                                .andExpect(status().isOk()));
        mvc.perform(
                        put("/api/roles/" + id)
                                .with(user("admin"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"ASSISTANT\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/roles/" + id).with(user("admin")).with(csrf()))
                .andExpect(status().isOk());
        Long builtIn = roles.findByNameIgnoreCase("CUSTOMER").orElseThrow().getId();
        mvc.perform(
                        put("/api/roles/" + builtIn)
                                .with(user("admin"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"NEWROLE\"}"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/roles/" + builtIn).with(user("admin")).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void productCrudSearchAndValidation() throws Exception {
        String request =
                "{\"name\":\"Bun\",\"price\":50.00,\"categoryId\":"
                        + category.getId()
                        + ",\"available\":true}";
        mvc.perform(
                        post("/api/products")
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isForbidden());
        long id =
                id(
                        mvc.perform(
                                        post("/api/products")
                                                .with(user("admin"))
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(request))
                                .andExpect(status().isCreated()));
        mvc.perform(
                        put("/api/products/" + id)
                                .with(user("admin"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request.replace("50.00", "60.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(60.00));
        mvc.perform(
                        get("/api/products")
                                .param("name", "bun")
                                .param("categoryId", category.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(
                        post("/api/products")
                                .with(user("admin"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request.replace("50.00", "-1")))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/products/" + id).with(user("admin")).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/products").param("name", "bun"))
                .andExpect(jsonPath("$.length()").value(0));
        assertThat(products.findById(id)).isPresent();
    }

    @Test
    void orderPricesAreSnapshotsAndOnlyPendingQuantitiesChange() throws Exception {
        long id = createOrder();
        Long itemId = orders.findById(id).orElseThrow().getItems().getFirst().getId();
        product.setPrice(new BigDecimal("999.00"));
        products.saveAndFlush(product);
        mvc.perform(
                        patch("/api/orders/" + id + "/items/" + itemId)
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(376.50));
        mvc.perform(
                        patch("/api/orders/" + id + "/status")
                                .with(user("admin"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk());
        mvc.perform(
                        patch("/api/orders/" + id + "/items/" + itemId)
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"quantity\":4}"))
                .andExpect(status().isConflict());
    }

    @Test
    void orderOwnershipAndAdministrativeStatusAreEnforced() throws Exception {
        long id = createOrder();
        mvc.perform(get("/api/orders/" + id).with(user("other"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/orders/" + id + "/cancel").with(user("other")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(
                        patch("/api/orders/" + id + "/status")
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/orders").with(user("customer"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/mine").with(user("customer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void completedOrdersCannotBeCancelledOrReopened() throws Exception {
        long id = createOrder();
        mvc.perform(
                        patch("/api/orders/" + id + "/status")
                                .with(user("admin"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict());
        for (String next : new String[] {"CONFIRMED", "PREPARING", "READY", "COMPLETED"}) {
            mvc.perform(
                            patch("/api/orders/" + id + "/status")
                                    .with(user("admin"))
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"status\":\"" + next + "\"}"))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/orders/" + id + "/cancel").with(user("customer")).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(
                        patch("/api/orders/" + id + "/status")
                                .with(user("admin"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isConflict());
        assertThat(orders.findById(id)).isPresent();
    }

    @Test
    void cancellingAndDeletingPreservesOrderHistory() throws Exception {
        long id = createOrder();
        mvc.perform(post("/api/orders/" + id + "/cancel").with(user("customer")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(delete("/api/products/" + product.getId()).with(user("admin")).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/users/" + customer.getId()).with(user("customer")).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/users/me").with(user("customer")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders/" + id).with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(251.00));
    }

    @Test
    void invalidAndUnavailableOrderItemsAreRejectedAtomically() throws Exception {
        for (String items :
                new String[] {
                    "[]", "[{\"productId\":" + product.getId() + ",\"quantity\":0}]", "[null]"
                }) {
            mvc.perform(
                            post("/api/orders")
                                    .with(user("customer"))
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"items\":" + items + "}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(
                        post("/api/orders")
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"items\":[{\"productId\":999999,\"quantity\":1}]}"))
                .andExpect(status().isNotFound());
        product.setAvailable(false);
        products.saveAndFlush(product);
        mvc.perform(
                        post("/api/orders")
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"items\":[{\"productId\":"
                                                + product.getId()
                                                + ",\"quantity\":1}]}"))
                .andExpect(status().isConflict());
        assertThat(orders.count()).isZero();
    }

    @Test
    void reviewsRequireOwnershipAndModerationControlsPublicAverage() throws Exception {
        long id = createReview();
        mvc.perform(get("/api/reviews")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/reviews/" + id).with(user("other")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/reviews/" + id)
                                .with(user("other"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"rating\":1,\"comment\":\"Changed\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/reviews/" + id).with(user("other")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(
                        patch("/api/reviews/" + id + "/moderation")
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        patch("/api/reviews/" + id + "/moderation")
                                .with(user("admin"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"APPROVED\",\"note\":\"Checked\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reviews/product/" + product.getId() + "/average"))
                .andExpect(jsonPath("$.averageRating").value(5.0));
        mvc.perform(
                        put("/api/reviews/" + id)
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"rating\":4,\"comment\":\"Updated review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(get("/api/reviews/product/" + product.getId() + "/average"))
                .andExpect(jsonPath("$.averageRating").value(0.0));
        mvc.perform(delete("/api/reviews/" + id).with(user("customer")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(reviews.findById(id)).isEmpty();
    }

    @Test
    void reviewValidationAndCustomerPrivacy() throws Exception {
        mvc.perform(
                        post("/api/reviews")
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"productId\":"
                                                + product.getId()
                                                + ",\"rating\":6,\"comment\":\"Bad rating\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/reviews")
                                .with(user("customer"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"productId\":999999,\"rating\":5,\"comment\":\"Missing"
                                            + " product\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/reviews/customer/" + other.getId()).with(user("customer")))
                .andExpect(status().isForbidden());
    }
}
