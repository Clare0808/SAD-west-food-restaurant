package com.westfood.backend;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {
    private final MongoStore store;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    private final String jwtSecret;

    public AccountController(MongoStore store, @Value("${JWT_SECRET:}") String jwtSecret) {
        this.store = store;
        this.jwtSecret = jwtSecret;
    }

    @GetMapping("/send-data")
    public List<Map<String, Object>> users() {
        return store.all("logins");
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, Object> input) {
        String email = text(input.get("email"));
        if (store.existsEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "該E-mail已存在!"));
        }
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("email", email);
        user.put("name", input.get("name"));
        user.put("number", input.get("number"));
        user.put("password", encoder.encode(text(input.get("password"))));
        user.put("role", "user");
        user.put("googleId", null);
        user.put("provider", "local");
        for (String flag : FLAGS) user.put(flag, false);
        store.save("logins", user);
        return ResponseEntity.ok(Map.of("success", true, "message", "註冊成功", "user",
                Map.of("email", email, "name", input.get("name"), "number", input.get("number"), "role", "user")));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> input) {
        Map<String, Object> user = store.find("logins", "email", text(input.get("email")));
        if (user == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "該E-mail不存在!"));
        Object password = user.get("password");
        if (!(password instanceof String hash) || !encoder.matches(text(input.get("password")), hash)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "密碼錯誤!"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "登入成功", "user",
                Map.of("email", user.get("email"), "name", user.get("name"), "role", user.get("role")),
                "token", token(user)));
    }

    @PostMapping("/now-login")
    public ResponseEntity<?> current(@RequestBody Map<String, Object> input) {
        Map<String, Object> user = store.find("logins", "email", text(input.get("email")));
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "用戶不存在"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.get("_id"));
        for (String flag : FLAGS) result.put(flag, user.getOrDefault(flag, false));
        return ResponseEntity.ok(Map.of("success", true, "user", result));
    }

    @PatchMapping("/modify-data")
    public ResponseEntity<?> update(@RequestBody Map<String, Object> input) {
        String column = text(input.get("column"));
        if (!List.of("name", "number", "email", "password", "dishClicked", "putInCartClicked",
                "orderClicked", "buyClicked", "reviewClicked", "serverClicked").contains(column)) {
            return ResponseEntity.badRequest().body(Map.of("message", "無效欄位"));
        }
        Object data = input.get("data");
        if ("password".equals(column)) data = encoder.encode(text(data));
        Map<String, Object> updated = store.update("logins", text(input.get("id")), column, data);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/remove-user")
    public Map<String, Object> remove(@RequestBody Map<String, List<?>> input) {
        store.delete("logins", input.get("list"));
        return Map.of("success", true, "message", "用戶已刪除");
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(HttpServletRequest request) {
        Object id = request.getSession(false) == null ? null : request.getSession(false).getAttribute("googleUserId");
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not logged in"));
        Map<String, Object> user = store.findId("logins", id.toString());
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not logged in"));
        return ResponseEntity.ok(Map.of("user", user, "email", user.get("email"), "token", token(user)));
    }

    @GetMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        return Map.of("message", "已登出");
    }

    String token(Map<String, Object> user) {
        if (jwtSecret.isBlank()) throw new IllegalStateException("JWT_SECRET must be configured");
        return JWT.create().withClaim("id", text(user.get("_id"))).withClaim("role", text(user.get("role")))
                .withExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS)).sign(Algorithm.HMAC256(jwtSecret));
    }

    static String text(Object value) { return Objects.toString(value, ""); }
    static final List<String> FLAGS = List.of("dishClicked", "putInCartClicked", "orderClicked", "buyClicked", "reviewClicked", "serverClicked");
}