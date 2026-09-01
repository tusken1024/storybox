package com.storybox.storybox.story.service;

import com.storybox.storybox.story.model.StoryAxis;
import com.storybox.storybox.story.model.StorySelection;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class StoryGenerator {

    private static final String SYSTEM_PROMPT = """
            Tu es un conteur pour enfants de 4 à 7 ans.
            Tu écris une courte histoire du soir en français, douce et rassurante.
            Règles :
            - 200 à 300 mots maximum.
            - Phrases courtes et simples.
            - Aucune violence, aucune peur durable, rien d'effrayant.
            - Une fin apaisante qui invite au sommeil.
            - Termine par une phrase douce, par exemple « Bonne nuit ».
            - Réponds UNIQUEMENT avec le texte de l'histoire, sans titre ni commentaire.
            """;

    private final ChatClient ollamaChat;
    private final ChatClient thauraChat;
    private final ChatClient chat;

    public StoryGenerator(
            @Qualifier("ollamaChatModel") ChatModel ollamaChatModel,
            @Qualifier("openAiChatModel") ChatModel thauraChat) {

        this.ollamaChat = ChatClient.builder(ollamaChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();

        this.thauraChat = ChatClient.builder(thauraChat)
                .defaultSystem(SYSTEM_PROMPT)
                .build();

        this.chat = this.ollamaChat;
    }

    public String generate(StorySelection selection) {
        String user = """
            Écris l'histoire avec ces ingrédients :
            - Héros : %s
            - Compagnon : %s
            - Lieu : %s
            - Objet : %s
            """.formatted(
                label(selection, StoryAxis.HERO),
                label(selection, StoryAxis.COMPANION),
                label(selection, StoryAxis.PLACE),
                label(selection, StoryAxis.OBJECT));

        return chat.prompt().user(user).call().content();
    }

    private String label(StorySelection selection, StoryAxis axis) {
        return selection.choices().get(axis).label();
    }
}