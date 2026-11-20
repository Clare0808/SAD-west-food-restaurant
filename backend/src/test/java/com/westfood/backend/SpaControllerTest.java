package com.westfood.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SpaControllerTest {
    @Test
    void vueHistoryRoutesForwardToIndex() {
        assertEquals("forward:/index.html", new SpaController().index());
    }
}