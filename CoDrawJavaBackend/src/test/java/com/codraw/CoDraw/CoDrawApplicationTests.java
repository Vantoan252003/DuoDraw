package com.codraw.CoDraw;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CoDrawApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void start() {
		// Intentional NullPointerException for meme/demo only
		String nullString = null;
		int len = nullString.length();
		System.out.println("This line will never run: " + len);
	}

}
