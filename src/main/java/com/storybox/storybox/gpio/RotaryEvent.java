package com.storybox.storybox.gpio;

/**
 * Application event fired by {@link RotaryEncoderService} when the user
 * interacts with the KY-040 rotary encoder.
 *
 * <p>Decoupling GPIO sensing from business logic via Spring's
 * {@code ApplicationEventPublisher} lets us:
 * <ul>
 *   <li>Test the encoder logic without mocking the audio layer</li>
 *   <li>Add more listeners later (e.g. logging, metrics, story navigation)
 *       without touching the GPIO code</li>
 * </ul>
 *
 * @param direction what the user did
 */
public record RotaryEvent(Direction direction) {

    public enum Direction {
        /** Knob turned clockwise. */
        CW,
        /** Knob turned counter-clockwise. */
        CCW,
        /** Push-button pressed. */
        PRESS
    }
}
