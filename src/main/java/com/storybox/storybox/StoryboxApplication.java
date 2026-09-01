package com.storybox.storybox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Storybox — AI-powered storyteller for kids.
 *
 * <ol>
 *   <li>Spring Boot runs on Raspberry Pi 4</li>
 *   <li>Audio plays through the speaker via {@code mpv}</li>
 *   <li>The KY-040 rotary encoder controls playback</li>
 * </ol>
 */
@SpringBootApplication
public class StoryboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoryboxApplication.class, args);
    }
}
