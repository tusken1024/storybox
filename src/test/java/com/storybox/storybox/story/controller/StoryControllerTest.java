package com.storybox.storybox.story.controller;

import com.storybox.storybox.story.model.StoryAxis;
import com.storybox.storybox.story.model.StoryChoice;
import com.storybox.storybox.story.model.StoryStatus;
import com.storybox.storybox.story.service.StoryEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StoryController.class)
class StoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoryEngine engine;

    @Test
    void shouldTurnClockwiseByDefaultWhenDirectionNotProvided() throws Exception {
        StoryStatus mockStatus = new StoryStatus(
                StoryEngine.Phase.CHOOSING,
                StoryAxis.HERO,
                new StoryChoice("chevalier", "Chevalier", "cues/hero_chevalier.mp3"),
                Map.of()
        );
        when(engine.status()).thenReturn(mockStatus);

        mockMvc.perform(post("/story/turn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("CHOOSING"))
                .andExpect(jsonPath("$.currentAxis").value("HERO"))
                .andExpect(jsonPath("$.currentChoice.id").value("chevalier"))
                .andExpect(jsonPath("$.currentChoice.label").value("Chevalier"));

        verify(engine).clockwise();
        verify(engine).status();
    }

    @Test
    void shouldTurnClockwiseWhenDirectionIsCw() throws Exception {
        StoryStatus mockStatus = new StoryStatus(
                StoryEngine.Phase.CHOOSING,
                StoryAxis.HERO,
                new StoryChoice("dragon", "Dragon", "cues/hero_dragon.mp3"),
                Map.of()
        );
        when(engine.status()).thenReturn(mockStatus);

        mockMvc.perform(post("/story/turn").param("direction", "cw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("CHOOSING"))
                .andExpect(jsonPath("$.currentAxis").value("HERO"))
                .andExpect(jsonPath("$.currentChoice.id").value("dragon"));

        verify(engine).clockwise();
        verify(engine).status();
    }

    @Test
    void shouldTurnCounterClockwiseWhenDirectionIsCcw() throws Exception {
        StoryStatus mockStatus = new StoryStatus(
                StoryEngine.Phase.CHOOSING,
                StoryAxis.HERO,
                new StoryChoice("princesse", "Princesse", "cues/hero_princesse.mp3"),
                Map.of()
        );
        when(engine.status()).thenReturn(mockStatus);

        mockMvc.perform(post("/story/turn").param("direction", "ccw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("CHOOSING"))
                .andExpect(jsonPath("$.currentAxis").value("HERO"))
                .andExpect(jsonPath("$.currentChoice.id").value("princesse"));

        verify(engine).counterClockwise();
        verify(engine).status();
    }

    @Test
    void shouldTurnCounterClockwiseWhenDirectionIsUppercaseCcw() throws Exception {
        StoryStatus mockStatus = new StoryStatus(
                StoryEngine.Phase.CHOOSING,
                StoryAxis.HERO,
                new StoryChoice("princesse", "Princesse", "cues/hero_princesse.mp3"),
                Map.of()
        );
        when(engine.status()).thenReturn(mockStatus);

        mockMvc.perform(post("/story/turn").param("direction", "CCW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("CHOOSING"))
                .andExpect(jsonPath("$.currentAxis").value("HERO"))
                .andExpect(jsonPath("$.currentChoice.id").value("princesse"));

        verify(engine).counterClockwise();
        verify(engine).status();
    }

    @Test
    void shouldHandlePressAndReturnStatus() throws Exception {
        StoryStatus mockStatus = new StoryStatus(
                StoryEngine.Phase.CHOOSING,
                StoryAxis.COMPANION,
                new StoryChoice("renard", "Renard", "cues/comp_renard.mp3"),
                Map.of(StoryAxis.HERO, new StoryChoice("chevalier", "Chevalier", "cues/hero_chevalier.mp3"))
        );
        when(engine.status()).thenReturn(mockStatus);

        mockMvc.perform(post("/story/press"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("CHOOSING"))
                .andExpect(jsonPath("$.currentAxis").value("COMPANION"))
                .andExpect(jsonPath("$.currentChoice.id").value("renard"))
                .andExpect(jsonPath("$.chosen.HERO.id").value("chevalier"));

        verify(engine).press();
        verify(engine).status();
    }

    @Test
    void shouldHandleResetAndReturnStatus() throws Exception {
        StoryStatus mockStatus = new StoryStatus(
                StoryEngine.Phase.IDLE,
                null,
                null,
                Map.of()
        );
        when(engine.status()).thenReturn(mockStatus);

        mockMvc.perform(post("/story/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("IDLE"))
                .andExpect(jsonPath("$.currentAxis").doesNotExist())
                .andExpect(jsonPath("$.currentChoice").doesNotExist());

        verify(engine).reset();
        verify(engine).status();
    }

    @Test
    void shouldReturnCurrentStatus() throws Exception {
        StoryStatus mockStatus = new StoryStatus(
                StoryEngine.Phase.IDLE,
                null,
                null,
                Map.of()
        );
        when(engine.status()).thenReturn(mockStatus);

        mockMvc.perform(get("/story/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("IDLE"));

        verify(engine).status();
    }
}
