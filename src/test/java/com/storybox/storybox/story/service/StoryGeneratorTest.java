package com.storybox.storybox.story.service;

import com.storybox.storybox.story.model.StoryAxis;
import com.storybox.storybox.story.model.StoryChoice;
import com.storybox.storybox.story.model.StorySelection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class StoryGeneratorTest {

    @Autowired
    private StoryGenerator storyGenerator;

    @Test
    void generate() {
        // Given
        Map<StoryAxis, StoryChoice> choices  = new HashMap<>();
        choices.put(StoryAxis.HERO, new StoryChoice("chevalier", "Chevalier",""));
        choices.put(StoryAxis.COMPANION, new StoryChoice("dragon", "Dragon",""));
        choices.put(StoryAxis.PLACE, new StoryChoice("chateau", "Chateau",""));
        choices.put(StoryAxis.OBJECT, new StoryChoice("epee", "",""));

        // When
        String storyText = storyGenerator.generate(new StorySelection(choices));

        // Then
        assertThat(storyText).isNotBlank();
    }
}