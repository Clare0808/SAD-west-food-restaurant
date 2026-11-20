package com.westfood.backend;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    private final MongoStore store;

    public SecurityConfig(MongoStore store) { this.store = store; }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ClientRegistrationRepository registrations,
            @Value("${FRONTEND_URL:}") String frontendUrl) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth -> oauth.authorizationEndpoint(endpoint -> endpoint
                        .authorizationRequestResolver(googleResolver(registrations)))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/auth-callback"))
                        .successHandler(successHandler(frontendUrl))
                        .failureHandler((request, response, error) -> response.sendRedirect(redirectBase(frontendUrl) + "/login")))
                .build();
    }

    private OAuth2AuthorizationRequestResolver googleResolver(ClientRegistrationRepository registrations) {
        DefaultOAuth2AuthorizationRequestResolver delegate =
                new DefaultOAuth2AuthorizationRequestResolver(registrations, "/api/auth-google");
        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                if (!request.getRequestURI().equals("/api/auth-google")) return null;
                OAuth2AuthorizationRequest requestDetails = delegate.resolve(request, "google");
                return requestDetails == null ? null : OAuth2AuthorizationRequest.from(requestDetails)
                        .additionalParameters(params -> params.put("prompt", "select_account")).build();
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
                return null;
            }
        };
    }

    private AuthenticationSuccessHandler successHandler(String frontendUrl) {
        return (HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.Authentication authentication) -> {
            org.springframework.security.oauth2.core.user.OAuth2User principal =
                    (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
            String email = principal.getAttribute("email");
            String googleId = principal.getAttribute("sub");
            if (email == null || googleId == null) {
                response.sendRedirect(redirectBase(frontendUrl) + "/login");
                return;
            }
            Map<String, Object> user = store.find("logins", "email", email);
            if (user == null) {
                Map<String, Object> created = new java.util.LinkedHashMap<>();
                created.put("email", email);
                created.put("name", principal.getAttribute("name"));
                created.put("password", null);
                created.put("googleId", googleId);
                created.put("provider", "google");
                created.put("role", "user");
                for (String flag : AccountController.FLAGS) created.put(flag, false);
                user = store.save("logins", created);
            } else {
                store.update("logins", user.get("_id").toString(), "googleId", googleId);
                store.update("logins", user.get("_id").toString(), "provider", "google");
            }
            request.getSession(true).setAttribute("googleUserId", user.get("_id"));
            response.sendRedirect(redirectBase(frontendUrl) + "/");
        };
    }

    private static String redirectBase(String frontendUrl) {
        if (!frontendUrl.isBlank()) return frontendUrl.replaceAll("/+$", "");
        // A relative redirect stays on the same origin (Render or the local dev proxy).
        return "";
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${FRONTEND_URL:}") String frontendUrl) {
        CorsConfiguration configuration = new CorsConfiguration();
        if (!frontendUrl.isBlank()) configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}