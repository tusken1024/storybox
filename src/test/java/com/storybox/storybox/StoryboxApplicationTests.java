package com.storybox.storybox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: the Spring context loads successfully under the dev profile
 * (which has GPIO disabled, so this test can run anywhere — including CI).
 */
@SpringBootTest
@ActiveProfiles("dev")
class StoryboxApplicationTests {

    @Test
    void contextLoads() {
        // If Spring can build the context with all our beans wired up,
        // this test passes. That alone catches 90% of refactor bugs.
    }
}
