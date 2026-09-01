package com.storybox.storybox.story.controller;

import com.storybox.storybox.story.service.StoryEngine;
import com.storybox.storybox.story.model.StoryStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Drives the engine over HTTP so you can validate the whole state machine on your
 * laptop with no GPIO
 *
 * <pre>
 *   curl -X POST "localhost:8080/story/press"          # IDLE -> start, hear first hero
 *   curl -X POST "localhost:8080/story/turn?dir=cw"    # next option on current axis
 *   curl -X POST "localhost:8080/story/turn?dir=ccw"   # previous option
 *   curl -X POST "localhost:8080/story/reset"          # back to idle
 *   curl -X GET  "localhost:8080/story/status"         # where am I?
 * </pre>
 *
 * A full run = press, (turn*, press) ×4 → the leaf MP3 plays.
 */
@RestController
@RequestMapping("/story")
public class StoryController {

    private final StoryEngine engine;

    public StoryController(StoryEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/turn")
    public StoryStatus turn(@RequestParam(defaultValue = "cw") String direction) throws IOException {
        if ("ccw".equalsIgnoreCase(direction)) {
            engine.counterClockwise();
        } else {
            engine.clockwise();
        }
        return engine.status();
    }

    @PostMapping("/press")
    public StoryStatus press() throws IOException {
        engine.press();
        return engine.status();
    }

    @PostMapping("/reset")
    public StoryStatus reset() throws IOException{
        engine.reset();
        return engine.status();
    }

    @GetMapping("/status")
    public StoryStatus status() {
        return engine.status();
    }
}
