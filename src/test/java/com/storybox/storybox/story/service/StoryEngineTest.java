package com.storybox.storybox.story.service;

import com.storybox.storybox.audio.AudioPlayer;
import com.storybox.storybox.story.StoryLibrary;
import com.storybox.storybox.story.model.Story;
import com.storybox.storybox.story.model.StoryAxis;
import com.storybox.storybox.story.model.StoryChoice;
import com.storybox.storybox.story.model.StorySelection;
import com.storybox.storybox.story.model.StoryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class StoryEngineTest {

    @Mock
    private StoryLibrary library;

    @Mock
    private AudioPlayer audio;

    private StoryEngine engine;

    private final StoryChoice hero1 = new StoryChoice("princesse", "Princesse", "cues/hero_princesse.mp3");
    private final StoryChoice hero2 = new StoryChoice("chevalier", "Chevalier", "cues/hero_chevalier.mp3");

    private final StoryChoice comp1 = new StoryChoice("renard", "Renard", "cues/comp_renard.mp3");
    private final StoryChoice comp2 = new StoryChoice("dragon", "Dragon", "cues/comp_dragon.mp3");

    private final StoryChoice place1 = new StoryChoice("foret", "Forêt", "cues/place_foret.mp3");
    private final StoryChoice place2 = new StoryChoice("chateau", "Château", "cues/place_chateau.mp3");

    private final StoryChoice obj1 = new StoryChoice("lanterne", "Lanterne", "cues/obj_lanterne.mp3");
    private final StoryChoice obj2 = new StoryChoice("epee", "Épée", "cues/obj_epee.mp3");

    @BeforeEach
    void setUp() {
        engine = new StoryEngine(library, audio);

        // Given
        lenient().when(library.choices(StoryAxis.HERO)).thenReturn(List.of(hero1, hero2));
        lenient().when(library.choices(StoryAxis.COMPANION)).thenReturn(List.of(comp1, comp2));
        lenient().when(library.choices(StoryAxis.PLACE)).thenReturn(List.of(place1, place2));
        lenient().when(library.choices(StoryAxis.OBJECT)).thenReturn(List.of(obj1, obj2));
    }

    @Nested
    @DisplayName("Initial state & IDLE behaviors")
    class IdleStateTests {

        @Test
        @DisplayName("Should start in IDLE phase with empty status")
        void shouldStartInIdlePhase() {
            // When
            StoryStatus status = engine.status();

            // Then
            assertThat(status.phase()).isEqualTo(StoryEngine.Phase.IDLE);
            assertThat(status.currentAxis()).isNull();
            assertThat(status.currentChoice()).isNull();
            assertThat(status.chosen()).isEmpty();
            verifyNoInteractions(audio, library);
        }

        @Test
        @DisplayName("Clockwise rotation in IDLE should have no effect")
        void clockwiseInIdleShouldDoNothing() throws IOException {
            // When
            engine.clockwise();
            StoryStatus status = engine.status();

            // Then
            assertThat(status.phase()).isEqualTo(StoryEngine.Phase.IDLE);
            verifyNoInteractions(audio, library);
        }

        @Test
        @DisplayName("Counter-clockwise rotation in IDLE should have no effect")
        void counterClockwiseInIdleShouldDoNothing() throws IOException {
            // When
            engine.counterClockwise();
            StoryStatus status = engine.status();

            // Then
            assertThat(status.phase()).isEqualTo(StoryEngine.Phase.IDLE);
            verifyNoInteractions(audio, library);
        }

        @Test
        @DisplayName("Pressing in IDLE should transition to CHOOSING HERO and announce first choice")
        void pressInIdleShouldTransitionToChoosingHero() throws IOException {
            // When
            engine.press();
            StoryStatus status = engine.status();

            // Then
            assertThat(status.phase()).isEqualTo(StoryEngine.Phase.CHOOSING);
            assertThat(status.currentAxis()).isEqualTo(StoryAxis.HERO);
            assertThat(status.currentChoice()).isEqualTo(hero1);
            assertThat(status.chosen()).isEmpty();

            verify(audio).play(hero1.cue());
        }
    }

    @Nested
    @DisplayName("Choosing phase navigation & carousel cycling")
    class ChoosingCarouselTests {

        @Test
        @DisplayName("Clockwise rotation advances cursor and plays audio cue")
        void clockwiseShouldAdvanceCursorAndAnnounce() throws IOException {
            // When
            engine.press(); // hero1

            engine.clockwise();
            StoryStatus status = engine.status();

            // Then
            assertThat(status.currentChoice()).isEqualTo(hero2);
            verify(audio).play(hero2.cue());

            // When
            engine.clockwise();
            status = engine.status();

            // Then
            assertThat(status.currentChoice()).isEqualTo(hero1);
            verify(audio, times(2)).play(hero1.cue());
        }

        @Test
        @DisplayName("Counter-clockwise rotation moves backward, wraps around, and plays audio cue")
        void counterClockwiseShouldMoveBackwardAndAnnounce() throws IOException {
            // When
            engine.press(); // hero1

            engine.counterClockwise();
            StoryStatus status = engine.status();

            // Then
            assertThat(status.currentChoice()).isEqualTo(hero2);
            verify(audio).play(hero2.cue());

            // When
            engine.counterClockwise();
            status = engine.status();

            // Then
            assertThat(status.currentChoice()).isEqualTo(hero1);
            verify(audio, times(2)).play(hero1.cue());
        }

    }

    @Nested
    @DisplayName("Full State Machine Flow: IDLE -> CHOOSING -> PLAYING -> IDLE")
    class FullStateMachineFlowTests {

        @Test
        @DisplayName("Complete end-to-end flow selecting options on all 4 axes, playing story, then pressing back to IDLE")
        void fullLifecycleThroughAllAxesToPlayingAndIdle() throws IOException {
            // Given
            Story expectedStory = new Story(
                    Map.of(StoryAxis.HERO, "chevalier",
                            StoryAxis.COMPANION, "renard",
                            StoryAxis.PLACE, "chateau",
                            StoryAxis.OBJECT, "lanterne"),
                    "L'aventure du chevalier",
                    "audio/stories/chevalier-renard-chateau-lanterne.mp3"
            );
            when(library.resolve(any(StorySelection.class))).thenReturn(expectedStory);

            // When
            // 1. Start from IDLE -> press -> CHOOSING (HERO)
            engine.press();

            // Then
            assertThat(engine.status().phase()).isEqualTo(StoryEngine.Phase.CHOOSING);
            assertThat(engine.status().currentAxis()).isEqualTo(StoryAxis.HERO);
            assertThat(engine.status().currentChoice()).isEqualTo(hero1);
            verify(audio).play(hero1.cue());

            // When
            // 2. HERO: Rotate to hero2 (Chevalier) and press to validate
            engine.clockwise();

            // Then
            assertThat(engine.status().currentChoice()).isEqualTo(hero2);
            verify(audio).play(hero2.cue());

            // When
            engine.press(); // Validate HERO -> advance to COMPANION

            // Then
            // Verify COMPANION axis reached
            assertThat(engine.status().phase()).isEqualTo(StoryEngine.Phase.CHOOSING);
            assertThat(engine.status().currentAxis()).isEqualTo(StoryAxis.COMPANION);
            assertThat(engine.status().currentChoice()).isEqualTo(comp1);
            assertThat(engine.status().chosen()).containsEntry(StoryAxis.HERO, hero2);
            verify(audio).play(comp1.cue());

            // When
            // 3. COMPANION: Keep comp1 (Renard) and validate
            engine.press(); // Validate COMPANION -> advance to PLACE

            // Then
            // Verify PLACE axis reached
            assertThat(engine.status().phase()).isEqualTo(StoryEngine.Phase.CHOOSING);
            assertThat(engine.status().currentAxis()).isEqualTo(StoryAxis.PLACE);
            assertThat(engine.status().currentChoice()).isEqualTo(place1);
            assertThat(engine.status().chosen())
                    .containsEntry(StoryAxis.HERO, hero2)
                    .containsEntry(StoryAxis.COMPANION, comp1);
            verify(audio).play(place1.cue());

            // When
            // 4. PLACE: Rotate to place2 (Château) and validate
            engine.clockwise();

            // Then
            assertThat(engine.status().currentChoice()).isEqualTo(place2);
            verify(audio).play(place2.cue());

            // When
            engine.press(); // Validate PLACE -> advance to OBJECT

            // Then
            // Verify OBJECT axis reached
            assertThat(engine.status().phase()).isEqualTo(StoryEngine.Phase.CHOOSING);
            assertThat(engine.status().currentAxis()).isEqualTo(StoryAxis.OBJECT);
            assertThat(engine.status().currentChoice()).isEqualTo(obj1);
            assertThat(engine.status().chosen())
                    .containsEntry(StoryAxis.HERO, hero2)
                    .containsEntry(StoryAxis.COMPANION, comp1)
                    .containsEntry(StoryAxis.PLACE, place2);
            verify(audio).play(obj1.cue());

            // When
            // 5. OBJECT: Validate obj1 (Lanterne) -> all axes complete -> PLAYING
            engine.press();

            // Then
            ArgumentCaptor<StorySelection> selectionCaptor = ArgumentCaptor.forClass(StorySelection.class);
            verify(library).resolve(selectionCaptor.capture());
            StorySelection capturedSelection = selectionCaptor.getValue();
            assertThat(capturedSelection.choices()).containsExactlyInAnyOrderEntriesOf(Map.of(
                    StoryAxis.HERO, hero2,
                    StoryAxis.COMPANION, comp1,
                    StoryAxis.PLACE, place2,
                    StoryAxis.OBJECT, obj1
            ));

            // When
            StoryStatus playingStatus = engine.status();

            // Then
            assertThat(playingStatus.phase()).isEqualTo(StoryEngine.Phase.PLAYING);
            assertThat(playingStatus.currentAxis()).isNull();
            assertThat(playingStatus.currentChoice()).isNull();
            assertThat(playingStatus.chosen()).containsExactlyInAnyOrderEntriesOf(Map.of(
                    StoryAxis.HERO, hero2,
                    StoryAxis.COMPANION, comp1,
                    StoryAxis.PLACE, place2,
                    StoryAxis.OBJECT, obj1
            ));
            verify(audio).play(expectedStory.audio());


            // When
            // 6. In PLAYING: rotating clockwise or counter-clockwise should do nothing
            clearInvocations(audio);
            engine.clockwise();
            engine.counterClockwise();

            // Then
            verifyNoInteractions(audio);
            assertThat(engine.status().phase()).isEqualTo(StoryEngine.Phase.PLAYING);

            // When
            // 7. In PLAYING: pressing stops audio and transitions back to IDLE
            engine.press();

            // Then
            verify(audio).stop();
            StoryStatus finalStatus = engine.status();
            assertThat(finalStatus.phase()).isEqualTo(StoryEngine.Phase.IDLE);
            assertThat(finalStatus.currentAxis()).isNull();
            assertThat(finalStatus.currentChoice()).isNull();
            assertThat(finalStatus.chosen()).isEmpty();
        }

        @Test
        @DisplayName("Pressing through all 4 axes back-to-back selects default first option on each axis")
        void pressThroughAllAxesBackToBack() throws IOException {
            // Given
            Story defaultStory = new Story(
                    Map.of(StoryAxis.HERO, "princesse",
                            StoryAxis.COMPANION, "renard",
                            StoryAxis.PLACE, "foret",
                            StoryAxis.OBJECT, "lanterne"),
                    "L'histoire par défaut",
                    "audio/stories/default.mp3"
            );
            when(library.resolve(any(StorySelection.class))).thenReturn(defaultStory);

            // When
            // Press 1: IDLE -> HERO (hero1)
            engine.press();
            // Press 2: Validate HERO -> COMPANION (comp1)
            engine.press();
            // Press 3: Validate COMPANION -> PLACE (place1)
            engine.press();
            // Press 4: Validate PLACE -> OBJECT (obj1)
            engine.press();
            // Press 5: Validate OBJECT -> PLAYING
            engine.press();

            // Then
            assertThat(engine.status().phase()).isEqualTo(StoryEngine.Phase.PLAYING);
            assertThat(engine.status().chosen()).containsExactlyInAnyOrderEntriesOf(Map.of(
                    StoryAxis.HERO, hero1,
                    StoryAxis.COMPANION, comp1,
                    StoryAxis.PLACE, place1,
                    StoryAxis.OBJECT, obj1
            ));
            verify(audio).play(defaultStory.audio());
        }
    }

    @Nested
    @DisplayName("Reset, Cancellation & Edge Cases")
    class ResetAndEdgeCasesTests {

        @Test
        @DisplayName("Reset during CHOOSING should stop audio and return to IDLE")
        void resetDuringChoosingShouldResetToIdle() throws IOException {
            // When
            engine.press(); // Start choosing HERO
            engine.clockwise(); // Move to hero2
            engine.reset();

            // Then
            verify(audio).stop();
            StoryStatus status = engine.status();
            assertThat(status.phase()).isEqualTo(StoryEngine.Phase.IDLE);
            assertThat(status.currentAxis()).isNull();
            assertThat(status.currentChoice()).isNull();
            assertThat(status.chosen()).isEmpty();
        }

        @Test
        @DisplayName("Reset during PLAYING should stop audio and return to IDLE")
        void resetDuringPlayingShouldResetToIdle() throws IOException {
            // Given
            when(library.resolve(any(StorySelection.class))).thenReturn(new Story(Map.of(), "Title", "audio.mp3"));

            // When
            // Fast forward to PLAYING
            engine.press(); // HERO
            engine.press(); // COMPANION
            engine.press(); // PLACE
            engine.press(); // OBJECT
            engine.press(); // PLAYING

            // Then
            assertThat(engine.status().phase()).isEqualTo(StoryEngine.Phase.PLAYING);

            // When
            engine.reset();

            // Then
            verify(audio).stop();
            StoryStatus status = engine.status();
            assertThat(status.phase()).isEqualTo(StoryEngine.Phase.IDLE);
            assertThat(status.chosen()).isEmpty();
        }
    }
}