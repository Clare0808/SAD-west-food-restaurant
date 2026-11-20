package com.westfood.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AccountControllerTest {
    private MongoStore store;
    private AccountController controller;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        store = mock(MongoStore.class);
        controller = new AccountController(store, "test-secret");
    }

    @Test
    void signupStoresBcryptAndCompatibleDefaults() {
        var response = controller.signup(Map.of("email", "person@example.com", "name", "Person", "number", "123", "password", "pass"));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked") var captured = (org.mockito.ArgumentCaptor<Map<String, Object>>) (org.mockito.ArgumentCaptor<?>)
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(store).save(eq("logins"), captured.capture());
        assertTrue(new BCryptPasswordEncoder().matches("pass", (String) captured.getValue().get("password")));
        assertEquals(false, captured.getValue().get("dishClicked"));
    }

    @Test
    void loginRecognizesExistingNodeBcryptHashesAndReturnsJwt() {
        var user = Map.<String, Object>of("_id", "507f1f77bcf86cd799439011", "email", "old@example.com",
                "name", "Old", "role", "user", "password", new BCryptPasswordEncoder().encode("secret"));
        when(store.find("logins", "email", "old@example.com")).thenReturn(user);
        var response = controller.login(Map.of("email", "old@example.com", "password", "secret"));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked") var body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("user", com.auth0.jwt.JWT.decode((String) body.get("token")).getClaim("role").asString());
    }

    @Test
    void rejectsDuplicateAndWrongPassword() {
        when(store.existsEmail("same@example.com")).thenReturn(true);
        assertEquals(HttpStatus.BAD_REQUEST, controller.signup(Map.of("email", "same@example.com")).getStatusCode());
        when(store.find("logins", "email", "same@example.com")).thenReturn(Map.of("password", new BCryptPasswordEncoder().encode("right")));
        assertEquals(HttpStatus.BAD_REQUEST, controller.login(Map.of("email", "same@example.com", "password", "wrong")).getStatusCode());
    }
}