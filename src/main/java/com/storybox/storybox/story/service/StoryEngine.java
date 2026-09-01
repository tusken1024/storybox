package com.storybox.storybox.story.service;

import com.storybox.storybox.audio.AudioPlayer;
import com.storybox.storybox.story.StoryLibrary;
import com.storybox.storybox.story.model.StorySelection;
import com.storybox.storybox.story.model.StoryStatus;
import com.storybox.storybox.story.model.Story;
import com.storybox.storybox.story.model.StoryAxis;
import com.storybox.storybox.story.model.StoryChoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;

/**
 * The heart of Stroybox: a small state machine driven by three inputs — clockwise,
 * counter-clockwise, press — exactly the three things a KY-040 can emit.
 *
 * <pre>
 *   IDLE               --press --> CHOOSING(HERO)
 *   CHOOSING(axis)     --turn  --> cycle options on this axis (announces each)
 *   CHOOSING(axis)     --press --> validate, advance to next axis
 *   CHOOSING(OBJECT)   --press --> resolve the last selection -and plays the story mp3
 *   PLAYING            --press --> stop & back to IDLE
 * </pre>
 *
 * <p>All transitions are {@code synchronized}: GPIO events and HTTP calls can
 * arrive on different threads.
 *
 * <p><b>Audio assumption (integration point):</b> uses {@code AudioPlayer#play(String)}
 * with barge-in semantics (a new play stops the previous) and {@code AudioPlayer#stop()}.
 */
@Service
public class StoryEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoryEngine.class);

    public enum Phase {IDLE, CHOOSING, PLAYING}

    private final StoryLibrary library;
    private final AudioPlayer audio;

    private Phase phase = Phase.IDLE;
    private int axisIndex = 0;   // index into StoryAxis.values()
    private int cursor = 0;      // position in the active axis carousel
    private final EnumMap<StoryAxis, StoryChoice> chosen = new EnumMap<>(StoryAxis.class);

    public StoryEngine(StoryLibrary library, AudioPlayer audio) {
        this.library = library;
        this.audio = audio;
    }

    public synchronized void clockwise() throws IOException {
        if (phase != Phase.CHOOSING) return;
        int n = currentOptions().size();
        cursor = (cursor + 1) % n;
        announce();
    }

    public synchronized void counterClockwise() throws IOException {
        if (phase != Phase.CHOOSING) return;
        int n = currentOptions().size();
        cursor = (cursor - 1 + n) % n;
        announce();
    }

    public synchronized void press() throws IOException {
        switch (phase) {
            case IDLE -> start();
            case CHOOSING -> validate();
            case PLAYING -> reset();
        }
    }

    /**
     * Hard reset to IDLE (also stops any playback). Exposed for the test harness.
     */
    public synchronized void reset() {
        audio.stop();
        chosen.clear();
        axisIndex = 0;
        cursor = 0;
        phase = Phase.IDLE;
        LOGGER.info("Reset — idle");
    }

    public synchronized StoryStatus status() {
        StoryChoice current = (phase == Phase.CHOOSING) ? currentOptions().get(cursor) : null;
        StoryAxis axis = (phase == Phase.CHOOSING) ? currentAxis() : null;
        return new StoryStatus(phase, axis, current, new EnumMap<>(chosen));
    }

    // ---- transitions -------------------------------------------------------

    private void start() throws IOException {
        chosen.clear();
        axisIndex = 0;
        cursor = 0;
        phase = Phase.CHOOSING;
        LOGGER.info("Start — choose your {}", currentAxis().label());
        announce();
    }

    private void validate() throws IOException {
        StoryChoice pick = currentOptions().get(cursor);
        chosen.put(currentAxis(), pick);
        LOGGER.info("Validated {} = {}", currentAxis().label(), pick.label());

        axisIndex++;
        cursor = 0;
        if (axisIndex < StoryAxis.values().length) {
            LOGGER.info("Now choose your {}", currentAxis().label());
            announce();
        } else {
            play();
        }
    }

    private void play() throws IOException {
        StorySelection selection = new StorySelection(chosen);
        Story story = library.resolve(selection);
        phase = Phase.PLAYING;
        LOGGER.info("Playing [{}] -> \"{}\" ({})", selection.summary(), story.title(), story.audio());
        audio.play(story.audio());
    }

    private void announce() throws IOException {
        audio.play(currentOptions().get(cursor).cue());
    }

    private StoryAxis currentAxis() {
        return StoryAxis.values()[axisIndex];
    }

    private List<StoryChoice> currentOptions() {
        List<StoryChoice> opts = library.choices(currentAxis());
        if (opts.isEmpty()) {
            throw new IllegalStateException("No choices defined for axis " + currentAxis());
        }
        return opts;
    }

    private synchronized void cancelPreparation() {
        reset();          // stops audio, back to IDLE
    }
}
