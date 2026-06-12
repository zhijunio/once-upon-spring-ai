///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//SOURCES RulesTools.java
//SOURCES PgVectorSupport.java
//REPOS mavencentral
//DEPS io.netty:netty-bom:4.2.9.Final@pom
//DEPS org.springframework.boot:spring-boot-starter-web:4.1.0
//DEPS org.springframework.boot:spring-boot-starter-jdbc:4.1.0
//DEPS org.springframework.ai:spring-ai-openai:2.0.0-RC2
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0-RC2
//DEPS org.springframework.ai:spring-ai-pgvector-store:2.0.0-RC2
//DEPS org.postgresql:postgresql:42.7.7
//DEPS com.zaxxer:HikariCP:6.2.1
//DEPS org.springaicommunity:spring-ai-a2a-server-autoconfigure:0.3.0
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.springframework:spring-core:7.0.3
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

package com.amazonaws;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.ai.vectorstore.VectorStore;

import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

/// Rules Agent — D&D 5e rules lookup via A2A protocol with pgvector RAG.
/// Prerequisites: docker compose up -d, CreateKnowledgeBase.java ingest complete.
/// Run with: jbang RulesAgent.java
@SpringBootApplication
public class RulesAgent {

    private static final String SYSTEM_PROMPT = """
        You are a D&D 5e rules expert. When asked about rules, ALWAYS use the queryDndRules tool
        to find the relevant rule from the official source — never answer from memory alone.
        Provide a clear, concise answer that includes:
        1. The rule mechanic (what dice to roll, what modifiers apply, DCs, etc.)
        2. The page reference from the source material
        3. Any relevant conditions, advantages, or disadvantages
        Keep responses focused, actionable, and ready for the Game Master to use in gameplay.
        """;

    public static void main(final String[] args) {
        // TODO 1: Configure Spring Boot properties for this A2A agent server.
        //   Use System.setProperty() to set four properties before SpringApplication.run():
        //     - Server port (this agent runs on port 8000)
        //     - Servlet context path (A2A convention uses "/a2a")
        //     - Application name
        //     - Enable the A2A server auto-configuration

        SpringApplication.run(RulesAgent.class, args);
    }

    // TODO 2: Create an AgentCard bean that describes this agent to the A2A network.
    // TODO 3: Create an AgentExecutor bean that wires the ChatClient with the RulesTools.
}

@Configuration
class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger("VectorStoreConfig");

    @Bean
    VectorStore vectorStore() {
        var apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("DASHSCOPE_API_KEY required for embedding queries");
        }
        log.info("PgVector store at {}", PgVectorSupport.jdbcUrl());
        var baseUrl = System.getenv().get("DASHSCOPE_BASE_URL");
        var client = new OpenAIClientImpl(ClientOptions.builder()
                .httpClient(SpringAiOpenAiHttpClient.builder().build())
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build());
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .openAiClient(client)
                .metadataMode(MetadataMode.EMBED)
                .options(OpenAiEmbeddingOptions.builder()
                        .model(System.getenv().getOrDefault("DASHSCOPE_EMBEDDING_MODEL", "text-embedding-v3"))
                        .dimensions(Integer.parseInt(
                                System.getenv().getOrDefault("DASHSCOPE_EMBEDDING_DIMENSIONS", "1024")))
                        .build())
                .build();
        return PgVectorSupport.vectorStore(embeddingModel);
    }
}

@Configuration
class RulesAgentConfig {

    @Bean
    ChatModel chatModel() {
        var apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set DASHSCOPE_API_KEY — create one at https://dashscope.console.aliyun.com/apiKey");
        }
        var baseUrl = System.getenv().get("DASHSCOPE_BASE_URL");
        var modelName = System.getenv().getOrDefault("DASHSCOPE_CHAT_MODEL", "qwen3.6-plus");
        var client = new OpenAIClientImpl(ClientOptions.builder()
                .httpClient(SpringAiOpenAiHttpClient.builder().build())
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build());
        return OpenAiChatModel.builder()
                .openAiClient(client)
                .openAiClientAsync(client.async())
                .options(OpenAiChatOptions.builder()
                        .model(modelName)
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .build())
                .build();
    }
}
