package com.westfood.backend;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class MenuControllerTest {
    @TempDir Path directory;

    @Test
    void addModifyDeleteMenuAndMoveUploadedImage() throws Exception {
        Path seed = Files.createDirectories(directory.resolve("seed/data"));
        Files.writeString(seed.resolve("dishData.json"), "[]");
        Path storage = directory.resolve("storage");
        MenuController menu = new MenuController(storage.toString(), directory.resolve("seed").toString(), new ObjectMapper());
        assertTrue(menu.list().isEmpty());
        var uploaded = menu.upload("New", new MockMultipartFile("image", "food.png", "image/png", new byte[] {1, 2, 3}));
        var input = Map.<String, Object>of("id", "1", "name", "New", "type", "rice", "price", "50",
                "description", "Food", "image", uploaded.get("tempPath"));
        menu.add(input);
        assertEquals("/images/rice/New.png", menu.list().get(0).get("image"));
        assertTrue(Files.isRegularFile(storage.resolve("images/rice/New.png")));
        menu.modify(Map.of("id", 1, "price", "60"));
        assertEquals(60.0, menu.list().get(0).get("price"));
        menu.remove(Map.of("list", List.of(1)));
        assertTrue(menu.list().isEmpty());
    }

    @Test
    void rejectsPathTraversalInUploads() throws Exception {
        MenuController menu = new MenuController(directory.toString(), directory.toString(), new ObjectMapper());
        assertThrows(IllegalArgumentException.class, () -> menu.upload("../unsafe", new MockMultipartFile("image", "a.png", "image/png", new byte[] {1})));
    }
}