package com.storybox.storybox.story.model;

/**
 * One option within an axis, e.g. {@code HERO}/"chevalier".
 *
 * @param id    stable identifier, used to build the selection key (e.g. "chevalier")
 * @param label human-readable name, used in logs / status (e.g. "Chevalier")
 * @param cue   short MP3 announced when the carousel lands on this option
 *              (relative to your audio dir, e.g. "cues/hero-chevalier.mp3")
 */
public record StoryChoice(String id, String label, String cue) {
}
