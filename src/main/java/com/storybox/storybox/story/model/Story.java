package com.storybox.storybox.story.model;

import java.util.Map;

/**
 * A static, pre-authored story bound to an exact combination of choices.
 *
 * @param selection axis → choice id (e.g. {HERO:"chevalier", COMPANION:"dragon", ...})
 * @param title     shown in logs / status
 * @param audio     MP3 played at the leaf (relative to your audio dir)
 */
public record Story(Map<StoryAxis, String> selection, String title, String audio) {
}
