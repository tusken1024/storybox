package com.storybox.storybox.story.controller;

import com.storybox.storybox.story.StoryLibrary;
import com.storybox.storybox.story.model.StoryAxis;
import com.storybox.storybox.story.model.StoryChoice;
import com.storybox.storybox.story.model.StorySelection;
import com.storybox.storybox.story.service.StoryGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StoryGeneratorController.class)
class StoryGeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoryGenerator storyGenerator;

    @MockitoBean
    private StoryLibrary storyLibrary;

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
        when(storyLibrary.choices(StoryAxis.HERO)).thenReturn(List.of(hero1, hero2));
        when(storyLibrary.choices(StoryAxis.COMPANION)).thenReturn(List.of(comp1, comp2));
        when(storyLibrary.choices(StoryAxis.PLACE)).thenReturn(List.of(place1, place2));
        when(storyLibrary.choices(StoryAxis.OBJECT)).thenReturn(List.of(obj1, obj2));
    }

    @Test
    void shouldGenerateStoryTextSuccessfully() throws Exception {
        when(storyGenerator.generate(any(StorySelection.class)))
                .thenReturn("Il était une fois un brave chevalier...");


        mockMvc.perform(post("/generator/text")
                        .param("hero", "chevalier")
                        .param("companion", "dragon")
                        .param("place", "chateau")
                        .param("object", "epee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selection").value("Chevalier · Dragon · Château · Épée"))
                .andExpect(jsonPath("$.text").value("Il était une fois un brave chevalier..."));

        verify(storyGenerator).generate(any(StorySelection.class));
    }

    @Test
    void shouldReturnBadRequestWhenUnknownChoiceProvided() throws Exception {
        mockMvc.perform(post("/generator/text")
                        .param("hero", "inconnu")
                        .param("companion", "dragon")
                        .param("place", "chateau")
                        .param("object", "epee"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown HERO id 'inconnu'"));

        verifyNoInteractions(storyGenerator);
    }

    @Test
    void shouldReturnInternalServerErrorWhenGeneratorFails() throws Exception {
        when(storyGenerator.generate(any(StorySelection.class)))
                .thenThrow(new RuntimeException("AI generation failed"));

        mockMvc.perform(post("/generator/text")
                        .param("hero", "chevalier")
                        .param("companion", "dragon")
                        .param("place", "chateau")
                        .param("object", "epee"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AI generation failed"));
    }

    @Test
    void shouldReturnBadRequestWhenRequiredParameterIsMissing() throws Exception {
        mockMvc.perform(post("/generator/text")
                        .param("hero", "chevalier")
                        .param("companion", "dragon")
                        .param("place", "chateau"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(storyGenerator);
    }
}