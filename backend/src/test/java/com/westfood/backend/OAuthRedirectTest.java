package com.westfood.backend;

import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

class OAuthRedirectTest {
    @Test
    void redirectsToSameOriginWhenFrontendUrlIsMissing() throws Exception {
        SecurityConfig config = new SecurityConfig(mock(MongoStore.class));
        var handler = (org.springframework.security.web.authentication.AuthenticationSuccessHandler)
                ReflectionTestUtils.invokeMethod(config, "successHandler", "");
        var user = mock(OAuth2User.class);
        var authentication = mock(Authentication.class);
        org.mockito.Mockito.when(authentication.getPrincipal()).thenReturn(user);
        var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);
        org.junit.jupiter.api.Assertions.assertEquals("/login", response.getRedirectedUrl());
    }
}