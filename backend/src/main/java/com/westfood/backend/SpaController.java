package com.westfood.backend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    // Serve Vue Router history URLs without intercepting API or static assets.
    @GetMapping({"/", "/login", "/menu", "/review", "/contact", "/cart", "/user",
            "/back-home", "/back-menu", "/back-order", "/back-review", "/back-chart",
            "/back-contact", "/back-user"})
    public String index() {
        return "forward:/index.html";
    }
}