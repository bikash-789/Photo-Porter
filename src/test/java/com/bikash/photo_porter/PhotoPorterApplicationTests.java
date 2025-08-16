package com.bikash.photo_porter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PhotoPorterApplicationTests {

    @Test
    void contextLoads() {
        // This test will fail if the application context cannot start
    }

    @Test
    void applicationStartsSuccessfully() {
        // Basic test to ensure the application can start
        PhotoPorterApplication.main(new String[]{});
    }
}
