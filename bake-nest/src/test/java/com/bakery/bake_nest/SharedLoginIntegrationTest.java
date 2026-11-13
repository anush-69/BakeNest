package com.bakery.bake_nest;

import com.bakery.bake_nest.model.Role;
import com.bakery.bake_nest.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SharedLoginIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filter;
    @Autowired EntityManager em;
    @Autowired PasswordEncoder encoder;

    @Test
    void databaseLoginStatusRolesAndSessionRevocation() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build();
        Role adminRole = new Role(); adminRole.setName("ADMIN"); em.persist(adminRole);
        Role userRole = new Role(); userRole.setName("USER"); em.persist(userRole);
        User admin = user("admin-test", "ACTIVE", adminRole);
        User customer = user("customer-test", "ACTIVE", userRole);
        User inactive = user("inactive-test", "INACTIVE", userRole);
        em.flush();
        loginFailure(mvc, customer.getUsername(), "wrong");
        loginFailure(mvc, inactive.getUsername(), "password");
        MockHttpSession userSession = login(mvc, customer.getUsername());
        MockHttpSession adminSession = login(mvc, admin.getUsername());
        mvc.perform(get("/api/users/me").session(userSession)).andExpect(status().isOk());
        mvc.perform(get("/api/admins").session(userSession)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admins").session(adminSession)).andExpect(status().isOk());
        customer.setPasswordHash(encoder.encode("new-password")); em.flush();
        mvc.perform(get("/api/users/me").session(userSession)).andExpect(status().isUnauthorized());
        admin.setStatus("INACTIVE"); em.flush();
        mvc.perform(get("/api/admins").session(adminSession)).andExpect(status().isUnauthorized());
    }
    private User user(String name, String status, Role role) {
        User user = new User(); user.setUsername(name); user.setEmail(name+"@example.invalid");
        user.setPasswordHash(encoder.encode("password")); user.setStatus(status);
        user.getRoles().add(role); em.persist(user); return user;
    }
    private MvcResult loginRequest(MockMvc mvc, String name, String password) throws Exception {
        MvcResult page = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        CsrfToken csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        return mvc.perform(post("/login").session((MockHttpSession)page.getRequest().getSession(false))
            .param("username",name).param("password",password)
            .param(csrf.getParameterName(),csrf.getToken())).andReturn();
    }
    private MockHttpSession login(MockMvc mvc, String name) throws Exception {
        MvcResult result = loginRequest(mvc,name,"password");
        org.junit.jupiter.api.Assertions.assertEquals("/", result.getResponse().getRedirectedUrl());
        return (MockHttpSession) result.getRequest().getSession(false);
    }
    private void loginFailure(MockMvc mvc, String name, String password) throws Exception {
        MvcResult result = loginRequest(mvc,name,password);
        org.junit.jupiter.api.Assertions.assertEquals("/login?error", result.getResponse().getRedirectedUrl());
    }
}
