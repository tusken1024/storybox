package com.storybox.storybox.story;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storybox.storybox.story.model.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads the story pack from a JSON resource and resolves a {@link StorySelection}
 * to a concrete {@link Story}, falling back to the pack's fallback story when the
 * exact combination hasn't been authored.
 *
 * <p>Pack location is configurable: {@code storybox.story.pack}
 * (default {@code classpath:stories.json}).
 */
@Component
public class StoryLibrary {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoryLibrary.class);

    private final ObjectMapper objectMapper;
    private final Resource packResource;

    private StoryPack pack;
    private Map<String, Story> storyMap = Map.of();

    public StoryLibrary(ObjectMapper objectMapper, @Value("${storybox.story.pack:classpath:stories.json}") Resource packResource) {
        this.objectMapper = objectMapper;
        this.packResource = packResource;
    }

    @PostConstruct
    void load() {
        try (InputStream in = packResource.getInputStream()) {
            this.pack = objectMapper.readValue(in, StoryPack.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read story pack " + packResource, e);
        }

        this.storyMap = pack.stories().stream()
                .collect(Collectors.toMap(StoryLibrary::keyOf, s -> s, (a, b) -> a));

        LOGGER.info("Loaded story pack '{}' — {} authored stories, fallback='{}'",
                pack.name(), storyMap.size(), pack.fallback().title());
    }

    private static String keyOf(Story story) {
        return Arrays.stream(StoryAxis.values())
                .map(axis -> story.selection().get(axis))
                .collect(Collectors.joining("-"));
    }

    /** Carousel options for an axis, in declared order. */
    public List<StoryChoice> choices(StoryAxis axis) {
        return pack.axes().getOrDefault(axis, List.of());
    }

    /** Resolve a full selection to a story, or the fallback if unauthored. */
    public Story resolve(StorySelection selection) {
        Story story = storyMap.get(selection.key());
        if (story == null) {
            LOGGER.info("No authored story for [{}] — using fallback", selection.summary());
            return pack.fallback();
        }
        return story;
    }

    public String packName() {
        return pack.name();
    }
}
