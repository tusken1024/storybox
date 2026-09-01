package com.storybox.storybox.audio;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.NoSuchFileException;
import java.util.Map;

/**
 * HTTP control surface for manual audio testing.
 *
 * <pre>{@code
 *   curl -X POST "http://localhost:8080/audio/play?file=test.mp3"
 *   curl -X POST "http://localhost:8080/audio/stop"
 *   curl "http://localhost:8080/audio/status"
 * }</pre>
 */
@RestController
@RequestMapping("/audio")
public class AudioController {

    private final AudioPlayer player;

    public AudioController(AudioPlayer player) {
        this.player = player;
    }

    @PostMapping("/play")
    public ResponseEntity<Map<String, Object>> play(@RequestParam String file) {
        try {
            player.play(file);
            return ResponseEntity.ok(Map.of("status", "playing", "file", file));
        } catch (NoSuchFileException e) {
            return ResponseEntity.status(404).body(Map.of("error", "file not found", "file", file));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/stop")
    public Map<String, Object> stop() {
        player.stop();
        return Map.of("status", "stopped");
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("playing", player.isPlaying());
    }
}
