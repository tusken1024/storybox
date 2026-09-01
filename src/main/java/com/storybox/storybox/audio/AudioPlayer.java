package com.storybox.storybox.audio;

import com.storybox.storybox.StoryboxProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plays audio files by shelling out to an external player (default: {@code mpv}).
 *
 * <p>Why shell-out instead of pure-Java decoding (JLayer, etc.):
 * <ul>
 *   <li>mpv handles every format we'll throw at it (mp3, wav, ogg, opus, m4a…)</li>
 *   <li>Hardware-accelerated audio on Pi out of the box</li>
 *   <li>Robust — no Java MP3 lib pitfalls, no codec licensing</li>
 * </ul>
 *
 * <p>Trade-off: depends on {@code mpv} being installed on the host
 * (installed via {@code sudo apt install mpv}).
 */
@Service
public class AudioPlayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioPlayer.class);

    private final StoryboxProperties properties;
    private Process currentProcess;

    public AudioPlayer(StoryboxProperties properties) {
        this.properties = properties;
    }

    /**
     * Plays the file (relative to the configured library directory),
     * stopping any currently-playing track first.
     *
     * @param fileName file name within {@code storybox.audio.library-dir}
     * @throws IOException                if mpv cannot be started
     * @throws java.nio.file.NoSuchFileException if the file doesn't exist
     */
    public synchronized void play(String fileName) throws IOException {
        Path file = properties.audio().libraryDir().resolve(fileName).normalize();

        // Guard against directory traversal: the resolved path must stay
        // within libraryDir.
        if (!file.startsWith(properties.audio().libraryDir())) {
            throw new IllegalArgumentException("Path escapes library dir: " + fileName);
        }
        if (!Files.exists(file)) {
            throw new java.nio.file.NoSuchFileException(file.toString());
        }

        stop();
        LOGGER.info("▶ Playing: {}", file);
        currentProcess = new ProcessBuilder(
                properties.audio().player(),
                "--no-video",
                "--really-quiet",
                file.toString())
                .redirectErrorStream(true)
                .start();
    }

    /** Stops any currently-playing audio. No-op if nothing is playing. */
    public synchronized void stop() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroy();
            LOGGER.info("⏹ Stopped current playback");
        }
        currentProcess = null;
    }

    /** @return {@code true} if a track is currently playing. */
    public synchronized boolean isPlaying() {
        return currentProcess != null && currentProcess.isAlive();
    }

    @PreDestroy
    void cleanup() {
        stop();
    }
}
