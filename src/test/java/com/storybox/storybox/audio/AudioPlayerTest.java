package com.storybox.storybox.audio;

import com.storybox.storybox.StoryboxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudioPlayerTest {

    @TempDir
    private Path libraryDir;

    private Path stubPlayer;

    private AudioPlayer player;

    @BeforeEach
    void setUp() throws IOException {
        // Fake "player": ignores --no-video --really-quiet <file> (shell scripts
        // don't parse args unless told to) and just stays alive for a bit.
        // Real OS process, no mpv/audio needed.
        stubPlayer = libraryDir.resolveSibling("stub-player.sh");
        Files.writeString(stubPlayer, "#!/bin/sh\nsleep 5\n");
        stubPlayer.toFile().setExecutable(true);

        var audio = new StoryboxProperties.Audio(libraryDir, stubPlayer.toString(), "fallback.mp3");
        var gpio = new StoryboxProperties.Gpio(22, 27, 17, false);

        player = new AudioPlayer(new StoryboxProperties(audio, gpio));

        Files.writeString(libraryDir.resolve("track.mp3"), "fake-mp3-bytes");
    }

    // --- Pure logic — no process spawned at all, these are free ---

    @Test
    void playingMissingFileThrowsNoSuchFileException() {
        // When Then
        assertThatThrownBy(() -> player.play("does-not-exist.mp3"))
                .isInstanceOf(NoSuchFileException.class);
    }

    @Test
    void playingOutsideLibraryDirIsRejected() {
        // When Then
        assertThatThrownBy(() -> player.play("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes library dir");
    }

    // --- Real process lifecycle, via the stub ---

    @Test
    void playStartsAProcessAndReportsPlaying() throws IOException {
        // When
        player.play("track.mp3");

        // Then
        assertThat(player.isPlaying()).isTrue();

        player.stop();
    }

    @Test
    void stopTerminatesTheProcess() throws IOException {
        // When
        player.play("track.mp3");
        player.stop();

        // Then
        assertThat(player.isPlaying()).isFalse();
    }

    @Test
    void secondPlayKillsThePreviousProcess() throws IOException, InterruptedException {
        // When
        player.play("track.mp3");
        player.play("track.mp3"); // barge-in
        Thread.sleep(100); // let the OS reap the killed one

        long stillRunning = ProcessHandle.allProcesses()
                .filter(p -> p.info().commandLine()
                        .map(cl -> cl.contains(stubPlayer.toString())).orElse(false))
                .count();

        // Then
        assertThat(stillRunning).isEqualTo(1); // only the second process survives

        player.stop();
    }
}