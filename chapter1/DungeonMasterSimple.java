///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral
//DEPS io.netty:netty-bom:4.2.9.Final@pom
//DEPS org.springframework.ai:spring-ai-openai:2.0.0-RC2
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0-RC2
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;

private static final Logger log = LoggerFactory.getLogger("DungeonMasterSimple");

void main(String[] args) {
    log.info("=== Starting Dungeon Master AI Agent ===");

    var apiKey = System.getenv("DASHSCOPE_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
        log.error("Set DASHSCOPE_API_KEY — create one at https://dashscope.console.aliyun.com/apiKey");
        return;
    }
    var baseUrl = System.getenv().get("DASHSCOPE_BASE_URL");
    var modelName = System.getenv().getOrDefault("DASHSCOPE_CHAT_MODEL", "qwen3.6-plus");

    var client = new OpenAIClientImpl(ClientOptions.builder()
            .httpClient(SpringAiOpenAiHttpClient.builder().build())
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .build());

    ChatModel chatModel = OpenAiChatModel.builder()
            .openAiClient(client)
            .openAiClientAsync(client.async())
            .options(OpenAiChatOptions.builder()
                    .model(modelName)
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .build())
            .build();

    // TODO 1: Build a ChatClient with a system prompt that sets the AI personality


    // TODO 2: Send a message to the agent and print the response

    log.info("\n=== Ending Dungeon Master AI Agent ===");
}

