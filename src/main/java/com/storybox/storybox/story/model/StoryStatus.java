package com.storybox.storybox.story.model;

import com.storybox.storybox.story.service.StoryEngine;

import java.util.Map;

/**
 * Snapshot of the engine state, returned by the HTTP test harness.
 *
 * @param phase         IDLE / CHOOSING / PLAYING
 * @param currentAxis   axis being chosen (null unless CHOOSING)
 * @param currentChoice option the carousel is currently on (null unless CHOOSING)
 * @param chosen        axes already validated, in order
 */
public record StoryStatus(StoryEngine.Phase phase,
                          StoryAxis currentAxis,
                          StoryChoice currentChoice,
                          Map<StoryAxis, StoryChoice> chosen) {
}
