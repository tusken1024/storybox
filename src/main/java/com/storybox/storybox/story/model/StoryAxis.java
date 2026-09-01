package com.storybox.storybox.story.model;

/**
 * The four axes the child navigates, in selection order
 * (Lunii-style: héros × compagnon × lieu × objet).
 *
 * <p>{@link #values()} order <b>is</b> the carousel order — don't reorder lightly.
 */
public enum StoryAxis {

    HERO("Héros"),
    COMPANION("Compagnon"),
    PLACE("Lieu"),
    OBJECT("Objet");

    private final String label;

    StoryAxis(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
