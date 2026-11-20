package com.westfood.backend;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

@WebMvcTest({AccountController.class, RecordsController.class, MenuController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "FRONTEND_URL=https://west-food-restaurant.onrender.com")
class HttpRoutesTest {
    @Autowired MockMvc mvc;
    @MockitoBean MongoStore store;

    @Test
    void recordsHaveOriginalPathsAndResponseShape() throws Exception {
        when(store.all("reviews")).thenReturn(List.of(Map.of("_id", "507f1f77bcf86cd799439011", "name", "Test")));
        mvc.perform(get("/api/get-review")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Test"));
        mvc.perform(post("/api/send-contact").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@b.com\",\"name\":\"A\",\"content\":\"Hi\",\"date\":\"2026\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.email").value("a@b.com"));
    }

    @Test
    void oauthStartUsesOriginalRoute() throws Exception {
        mvc.perform(get("/api/auth-google")).andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("accounts.google.com")));
        mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
    }
}