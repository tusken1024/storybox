package com.storybox.storybox.story.controller;

import com.storybox.storybox.story.StoryLibrary;
import com.storybox.storybox.story.model.StoryAxis;
import com.storybox.storybox.story.model.StoryChoice;
import com.storybox.storybox.story.model.StorySelection;
import com.storybox.storybox.story.service.StoryGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.Map;

/**
 * HTTP control surface for manually exercising story generation lets you test the AI text pipeline
 * without driving the carousel through {@code /story/*}.
 *
 * <pre>{@code
 *   curl -X POST "localhost:8080/generator/text?hero=chevalier&companion=dragon&place=chateau&object=epee"
 *   curl -X POST "localhost:8080/generator/text?hero=princesse&companion=renard&place=foret&object=lanterne"
 * }</pre>
 */
@RestController
@RequestMapping("/generator")
public class StoryGeneratorController {

    private final StoryLibrary library;
    private final StoryGenerator generator;

    public StoryGeneratorController(StoryLibrary library, StoryGenerator generator) {
        this.library = library;
        this.generator = generator;
    }

    @PostMapping("/text")
    public ResponseEntity<Map<String, Object>> text(
            @RequestParam String hero,
            @RequestParam String companion,
            @RequestParam String place,
            @RequestParam String object) {
        StorySelection selection;
        try {
            selection = resolve(hero, companion, place, object);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        try {
            String text = generator.generate(selection);
            return ResponseEntity.ok(Map.of("selection", selection.summary(), "text", text));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private StorySelection resolve(String hero, String companion, String place, String object) {
        Map<StoryAxis, StoryChoice> choices = new EnumMap<>(StoryAxis.class);
        choices.put(StoryAxis.HERO, choiceOf(StoryAxis.HERO, hero));
        choices.put(StoryAxis.COMPANION, choiceOf(StoryAxis.COMPANION, companion));
        choices.put(StoryAxis.PLACE, choiceOf(StoryAxis.PLACE, place));
        choices.put(StoryAxis.OBJECT, choiceOf(StoryAxis.OBJECT, object));
        return new StorySelection(choices);
    }

    private StoryChoice choiceOf(StoryAxis axis, String id) {
        return library.choices(axis).stream()
                .filter(choice -> choice.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown %s id '%s'".formatted(axis, id)));
    }
}

