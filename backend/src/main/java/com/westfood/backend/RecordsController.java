package com.westfood.backend;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RecordsController {
    private final MongoStore store;
    public RecordsController(MongoStore store) { this.store = store; }

    @GetMapping("/get-orders") public List<Map<String, Object>> orders() { return store.all("orders"); }
    @GetMapping("/get-buy-orders") public List<Map<String, Object>> buys() { return store.all("buys"); }
    @GetMapping("/get-review") public List<Map<String, Object>> reviews() { return store.all("reviews"); }
    @GetMapping("/get-contact") public List<Map<String, Object>> contacts() { return store.all("contacts"); }

    @PostMapping("/send-orders")
    public Map<String, Object> addOrder(@RequestBody Map<String, Object> input) {
        Map<String, Object> value = pick(input, "email", "name", "amount", "price", "image", "check", "total");
        store.save("orders", value);
        return Map.of("success", true, "message", "訂單已保存", "order", value);
    }

    @PostMapping("/send-buy-orders")
    public Map<String, Object> addBuy(@RequestBody Map<String, Object> input) {
        Map<String, Object> value = pick(input, "email", "list", "total", "code", "date");
        store.save("buys", value);
        return Map.of("success", true, "message", "訂單已保存", "buy", pick(input, "email", "list", "total", "code", "date"));
    }

    @PostMapping("/send-review")
    public Map<String, Object> addReview(@RequestBody Map<String, Object> input) {
        Map<String, Object> value = pick(input, "name", "stars", "content", "date");
        store.save("reviews", value);
        return Map.of("success", true, "message", "評論留言成功", "user", value);
    }

    @PostMapping("/send-contact")
    public Map<String, Object> addContact(@RequestBody Map<String, Object> input) {
        Map<String, Object> value = pick(input, "email", "name", "content", "date");
        store.save("contacts", value);
        return Map.of("success", true, "message", "留言傳送成功", "user", value);
    }

    @PostMapping("/remove-orders") public Map<String, Object> removeOrders(@RequestBody Map<String, List<?>> input) { return remove("orders", input, "訂單已刪除"); }
    @PostMapping("/remove-buy-orders") public Map<String, Object> removeBuys(@RequestBody Map<String, List<?>> input) { return remove("buys", input, "訂單已刪除"); }
    @PostMapping("/remove-review") public Map<String, Object> removeReviews(@RequestBody Map<String, List<?>> input) { return remove("reviews", input, "評論已刪除"); }
    @PostMapping("/remove-contact") public Map<String, Object> removeContacts(@RequestBody Map<String, List<?>> input) { return remove("contacts", input, "留言已刪除"); }

    private Map<String, Object> remove(String collection, Map<String, List<?>> input, String message) {
        store.delete(collection, input.get("list"));
        return Map.of("success", true, "message", message);
    }

    private Map<String, Object> pick(Map<String, Object> input, String... fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String field : fields) if (input.containsKey(field)) result.put(field, input.get(field));
        return result;
    }
}