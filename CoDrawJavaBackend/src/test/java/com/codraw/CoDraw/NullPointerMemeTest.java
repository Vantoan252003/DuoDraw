package com.codraw.CoDraw;

import org.junit.jupiter.api.Test;

class NullPointerMemeTest {

    @Test
    void reproduceNullPointerException() {
        String oops = null;
        // intentionally trigger NullPointerException for meme/demo
        int length = oops.length();
        System.out.println("length=" + length);
    }
}
