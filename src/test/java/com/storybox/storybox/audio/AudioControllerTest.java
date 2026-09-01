package com.storybox.storybox.audio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.NoSuchFileException;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AudioController.class)
class AudioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AudioPlayer audioPlayer;

    @Test
    void shouldReturnPlayingStatusWhenPlayIsCalled() throws Exception {
        doNothing().when(audioPlayer).play("test.mp3");

        mockMvc.perform(post("/audio/play").param("file", "test.mp3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("playing"))
                .andExpect(jsonPath("$.file").value("test.mp3"));

        verify(audioPlayer).play("test.mp3");
    }

    @Test
    void shouldReturnNotFoundWhenAudioFileNotFound() throws Exception {
        doThrow(new NoSuchFileException("missing.mp3")).when(audioPlayer).play("missing.mp3");

        mockMvc.perform(post("/audio/play").param("file", "missing.mp3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("file not found"))
                .andExpect(jsonPath("$.file").value("missing.mp3"));

        verify(audioPlayer).play("missing.mp3");
    }

    @Test
    void shouldReturnInternalServerErrorWhenPlaybackFails() throws Exception {
        doThrow(new RuntimeException("Playback failed")).when(audioPlayer).play("corrupt.mp3");

        mockMvc.perform(post("/audio/play").param("file", "corrupt.mp3"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Playback failed"));

        verify(audioPlayer).play("corrupt.mp3");
    }

    @Test
    void shouldReturnBadRequestWhenFileParameterIsMissing() throws Exception {
        mockMvc.perform(post("/audio/play"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(audioPlayer);
    }

    @Test
    void shouldReturnStoppedStatusWhenStopIsCalled() throws Exception {
        mockMvc.perform(post("/audio/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("stopped"));

        verify(audioPlayer).stop();
    }

    @Test
    void shouldReturnPlayingTrueWhenAudioIsPlaying() throws Exception {
        when(audioPlayer.isPlaying()).thenReturn(true);

        mockMvc.perform(get("/audio/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playing").value(true));

        verify(audioPlayer).isPlaying();
    }

    @Test
    void shouldReturnPlayingFalseWhenAudioIsNotPlaying() throws Exception {
        when(audioPlayer.isPlaying()).thenReturn(false);

        mockMvc.perform(get("/audio/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playing").value(false));

        verify(audioPlayer).isPlaying();
    }
}
