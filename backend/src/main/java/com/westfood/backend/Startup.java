package com.westfood.backend;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Startup implements ApplicationRunner {
    private final MongoStore store;
    public Startup(MongoStore store) { this.store = store; }

    @Override
    public void run(ApplicationArguments args) {
        if (store.find("logins", "role", "admin") != null) return;
        Map<String, Object> admin = new LinkedHashMap<>();
        admin.put("email", "admin@example.com");
        admin.put("name", "管理者");
        admin.put("number", "00000000");
        admin.put("password", new BCryptPasswordEncoder(10).encode("admin123"));
        admin.put("role", "admin");
        admin.put("provider", "local");
        admin.put("googleId", null);
        for (String flag : AccountController.FLAGS) admin.put(flag, false);
        store.save("logins", admin);
    }
}