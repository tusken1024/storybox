package com.storybox.storybox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Centralized configuration for the Storybox application.
 *
 * <p>Bound from {@code application.properties} under the {@code storybox} prefix.
 *
 * @param audio   audio player configuration
 * @param gpio    GPIO pin assignments for the rotary encoder
 */
@ConfigurationProperties(prefix = "storybox")
public record StoryboxProperties(Audio audio, Gpio gpio) {

    /**
     * @param libraryDir directory containing playable audio files (mp3, wav…)
     * @param player     external player command (e.g. {@code mpv}, {@code cvlc})
     * @param defaultTrack file name used mainly for test
     */
    public record Audio(Path libraryDir, String player, String defaultTrack) {}

    /**
     * BCM pin numbers (NOT physical pin numbers).
     * Defaults match the wiring diagram: CLK=22, DT=27, SW=17.
     *
     * @param clkPin GPIO BCM pin connected to KY-040 CLK
     * @param dtPin  GPIO BCM pin connected to KY-040 DT
     * @param swPin  GPIO BCM pin connected to KY-040 SW (push button)
     * @param enabled disable the whole GPIO subsystem (useful when running on a laptop)
     */
    public record Gpio(int clkPin, int dtPin, int swPin, boolean enabled) {}
}
