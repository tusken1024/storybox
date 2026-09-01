package com.storybox.storybox.story.model;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The accumulated choices once all four axes have been validated.
 */
public record StorySelection(Map<StoryAxis, StoryChoice> choices) {

    public StorySelection {
        // defensive copy + guaranteed axis ordering
        choices = new EnumMap<>(choices);
    }

    /** Stable key in axis order, e.g. {@code "chevalier|dragon|chateau|epee"}. */
    public String key() {
        return Arrays.stream(StoryAxis.values())
                .map(axis -> choices.get(axis).id())
                .collect(Collectors.joining("-"));
    }

    /** Human-readable, for logs, e.g. {@code "Chevalier · Dragon · Château · Épée"}. */
    public String summary() {
        return Arrays.stream(StoryAxis.values())
                .map(axis -> choices.get(axis).label())
                .collect(Collectors.joining(" · "));
    }
}
