package com.storybox.storybox.story;

import com.storybox.storybox.gpio.RotaryEvent;
import com.storybox.storybox.story.service.StoryEngine;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Glue between the physical KY-040 and the {@link StoryEngine}.
 */
@Component
public class StoryEngineBridge {

    private final StoryEngine engine;

    public StoryEngineBridge(StoryEngine engine) {
        this.engine = engine;
    }

    @EventListener
    public void onRotary(RotaryEvent event) throws IOException {
        switch (event.direction()) {                 // <-- adapt to your RotaryEvent
            case CW -> engine.clockwise();
            case CCW -> engine.counterClockwise();
            case PRESS -> engine.press();
        }
    }
}
