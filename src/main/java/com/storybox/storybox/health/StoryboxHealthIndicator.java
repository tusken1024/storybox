package com.storybox.storybox.health;

import com.storybox.storybox.StoryboxProperties;
import com.storybox.storybox.audio.AudioPlayer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;

/**
 * Custom Actuator health indicator exposing the things that actually
 * matter for Storybox: is the audio library reachable, is the player
 * currently playing.
 *
 * <p>Visible at {@code http://localhost:8080/actuator/health}.
 */
@Component
public class StoryboxHealthIndicator implements HealthIndicator {

    private final StoryboxProperties properties;
    private final AudioPlayer player;

    public StoryboxHealthIndicator(StoryboxProperties properties, AudioPlayer player) {
        this.properties = properties;
        this.player = player;
    }

    @Override
    public Health health() {
        boolean libraryOk = Files.isDirectory(properties.audio().libraryDir());

        Health.Builder builder = libraryOk ? Health.up() : Health.outOfService();
        return builder
                .withDetail("libraryDir", properties.audio().libraryDir().toString())
                .withDetail("libraryAccessible", libraryOk)
                .withDetail("player", properties.audio().player())
                .withDetail("gpioEnabled", properties.gpio().enabled())
                .withDetail("currentlyPlaying", player.isPlaying())
                .build();
    }
}
