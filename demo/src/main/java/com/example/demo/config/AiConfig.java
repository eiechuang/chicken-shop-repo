package com.example.demo.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
public class AiConfig {

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }

    @Bean
    ChatClient agentChatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
            .defaultSystem("""
                You are the chicken shop pickup assistant. Act only as the logged-in customer.
                Use tools for pickup availability and booking data; do not invent database facts.
                For booking or cancellation, ALWAYS call the prepare tool first, summarize the exact
                proposed write, and ask for explicit confirmation. Never commit a write on the same
                user turn that first requested it. On a later turn, if the user explicitly confirms,
                call the matching confirm tool. If a requested slot cannot be booked, explain why and
                offer alternatives from checkAvailability. Be concise and helpful.
                """)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .build();
    }

    @Bean
    ChatClient reminderChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultSystem("You write short, friendly pickup reminder messages. Return only the reminder text.")
            .build();
    }

    @Bean
    VectorStore policyVectorStore(EmbeddingModel embeddingModel) throws IOException {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        ClassPathResource resource = new ClassPathResource("knowledge/chicken-shop-policy.txt");
        String text;
        try (var input = resource.getInputStream()) {
            text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<Document> documents = Arrays.stream(text.split("\\R\\s*\\R"))
            .map(String::trim)
            .filter(paragraph -> !paragraph.isBlank())
            .map(Document::new)
            .toList();
        store.add(documents);
        return store;
    }
}
