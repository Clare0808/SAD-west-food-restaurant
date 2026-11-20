package com.westfood.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MenuController {
    private final Path dataDir;
    private final Path seedDir;
    private final ObjectMapper mapper;

    public MenuController(@Value("${MENU_DATA_DIR:../public}") String dataDir,
            @Value("${MENU_SEED_DIR:../public}") String seedDir, ObjectMapper mapper) {
        this.dataDir = Path.of(dataDir).toAbsolutePath().normalize();
        this.seedDir = Path.of(seedDir).toAbsolutePath().normalize();
        this.mapper = mapper;
    }

    @GetMapping("/data/dishData.json")
    public synchronized List<Map<String, Object>> list() throws IOException {
        Path target = dataDir.resolve("data/dishData.json");
        if (!Files.exists(target)) return mapper.readValue(seedDir.resolve("data/dishData.json").toFile(), new TypeReference<>() {});
        return mapper.readValue(target.toFile(), new TypeReference<>() {});
    }

    @GetMapping("/images/**")
    public ResponseEntity<byte[]> image(jakarta.servlet.http.HttpServletRequest request) throws IOException {
        String relative = request.getRequestURI().substring(1);
        if (relative.indexOf('%') >= 0) relative = java.net.URLDecoder.decode(relative, java.nio.charset.StandardCharsets.UTF_8);
        if (relative.indexOf('\\') >= 0 || relative.contains("..")) return ResponseEntity.badRequest().build();
        Path path = safeResolve(dataDir, relative);
        if (!Files.isRegularFile(path) && !relative.startsWith("images/_temp/")) path = safeResolve(seedDir, relative);
        if (!Files.isRegularFile(path)) return ResponseEntity.notFound().build();
        String mime = Files.probeContentType(path);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime == null ? "application/octet-stream" : mime))
                .body(Files.readAllBytes(path));
    }

    @PostMapping(value = "/api/upload-img", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("name") String name, @RequestParam("image") MultipartFile image) throws IOException {
        String filename = safePart(name) + extension(image.getOriginalFilename());
        Path target = safeResolve(dataDir, "images/_temp/" + filename);
        Files.createDirectories(target.getParent());
        image.transferTo(target);
        return Map.of("success", true, "tempPath", "/images/_temp/" + filename);
    }

    @PostMapping("/api/add-menu")
    public synchronized Map<String, Object> add(@RequestBody Map<String, Object> input) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>(list());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", Integer.parseInt(AccountController.text(input.get("id"))));
        item.put("name", input.get("name"));
        item.put("category", input.get("type"));
        item.put("price", Double.valueOf(AccountController.text(input.get("price"))));
        item.put("description", input.get("description"));
        item.put("image", finalImage(input.get("image"), input.get("type"), input.get("name")));
        data.add(item);
        write(data);
        return Map.of("success", true, "message", "餐點已新增", "item", item);
    }

    @PostMapping("/api/modify-menu")
    public synchronized Map<String, Object> modify(@RequestBody Map<String, Object> input) throws IOException {
        List<Map<String, Object>> data = list();
        int id = Integer.parseInt(AccountController.text(input.get("id")));
        for (Map<String, Object> item : data) {
            if (((Number) item.get("id")).intValue() != id) continue;
            String category = nonempty(input.get("type")) ? AccountController.text(input.get("type")) : AccountController.text(item.get("category"));
            if (nonempty(input.get("image")) && AccountController.text(input.get("image")).startsWith("/images/_temp/")) {
                item.put("image", finalImage(input.get("image"), category, item.get("name")));
            }
            if (nonempty(input.get("name"))) item.put("name", input.get("name"));
            if (nonempty(input.get("type"))) item.put("category", input.get("type"));
            if (nonempty(input.get("price"))) item.put("price", Double.valueOf(AccountController.text(input.get("price"))));
            if (nonempty(input.get("description"))) item.put("description", input.get("description"));
        }
        write(data);
        return Map.of("success", true, "message", "餐點已修改");
    }

    @PostMapping("/api/remove-menu")
    public synchronized Map<String, Object> remove(@RequestBody Map<String, List<?>> input) throws IOException {
        int id = Integer.parseInt(AccountController.text(input.get("list").get(0)));
        List<Map<String, Object>> data = list();
        data.removeIf(item -> ((Number) item.get("id")).intValue() == id);
        write(data);
        return Map.of("success", true, "message", "餐點已刪除");
    }

    private void write(List<Map<String, Object>> data) throws IOException {
        Path target = dataDir.resolve("data/dishData.json");
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), "menu-", ".json");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), data);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }

    private String finalImage(Object image, Object category, Object name) throws IOException {
        String imageUrl = AccountController.text(image);
        if (!imageUrl.startsWith("/images/_temp/")) return imageUrl;
        String file = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        String destination = "images/" + safePart(AccountController.text(category)) + "/" + safePart(AccountController.text(name)) + extension(file);
        Path src = safeResolve(dataDir, "images/_temp/" + safePart(file));
        Path dest = safeResolve(dataDir, destination);
        Files.createDirectories(dest.getParent());
        Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
        return "/" + destination;
    }

    private static Path safeResolve(Path base, String relative) {
        Path resolved = base.resolve(relative).normalize();
        if (!resolved.startsWith(base)) throw new IllegalArgumentException("Invalid path");
        return resolved;
    }

    private static String safePart(String text) {
        if (text.isBlank() || text.equals(".") || text.equals("..") || text.contains("/") || text.contains("\\"))
            throw new IllegalArgumentException("Invalid filename");
        return text;
    }

    private static String extension(String name) {
        String file = AccountController.text(name);
        int dot = file.lastIndexOf('.');
        String ext = dot < 0 ? "" : file.substring(dot).toLowerCase();
        if (!List.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(ext)) throw new IllegalArgumentException("Invalid image extension");
        return ext;
    }

    private static boolean nonempty(Object value) { return value != null && !AccountController.text(value).isBlank(); }
}