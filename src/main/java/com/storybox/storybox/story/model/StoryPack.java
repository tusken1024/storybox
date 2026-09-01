package com.storybox.storybox.story.model;

import java.util.List;
import java.util.Map;

/**
 * Root of {@code stories.json}.
 *
 * @param version  pack schema version (bump if you change the format)
 * @param name     pack display name
 * @param axes     for each axis, the ordered list of choices in the carousel
 * @param stories  authored stories, each bound to an exact selection
 * @param fallback played when a combination has no authored story
 *                 (you can't author all 16+ combos by hand — this saves the UX)
 */
public record StoryPack(
        int version,
        String name,
        Map<StoryAxis, List<StoryChoice>> axes,
        List<Story> stories,
        Story fallback) {
}
