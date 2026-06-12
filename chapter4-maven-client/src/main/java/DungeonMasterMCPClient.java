import java.util.Set;

import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

private static final Logger log = LoggerFactory.getLogger("DungeonMasterMCPClient");

void main(String[] args) {
    var apiKey = System.getenv("DASHSCOPE_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
        log.error("Set DASHSCOPE_API_KEY — create one at https://dashscope.console.aliyun.com/apiKey");
        return;
    }

    log.info("Connecting to D&D Dice Roll MCP Server...");

    // TODO 1: Create the HTTP transport and MCP client

    try {
        // TODO 2: Initialize the MCP client, discover tools, and bridge them to Spring AI.

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

        var agent = ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are Lady Luck, the mystical keeper of dice and fortune in D&D adventures.
                        You speak with theatrical flair and always announce dice rolls with appropriate drama.
                        You know all about D&D mechanics, always use the appropriate tools when applicable - never make up results!
                        """)
                .build();

        IO.println("\n\uD83C\uDFB2 Lady Luck - D&D Gamemaster with MCP Dice Rolling");
        IO.println("=".repeat(60));
        IO.println("\n\uD83C\uDFAF Try: 'Roll a d20' or 'Roll a d6' or 'Roll a d100'");

        var exitCommands = Set.of("exit", "quit", "bye");

        while (true) {
            var userInput = IO.readln("\n\uD83C\uDFB2 Your request: ").trim();

            if (exitCommands.contains(userInput.toLowerCase())) {
                IO.println("\uD83C\uDFAD May fortune favor your future adventures!");
                break;
            }

            IO.println("\n\uD83C\uDFB2 Rolling the dice of fate...\n");

            try {
                var response = agent.prompt()
                        .user(userInput)
                        .call()
                        .content();

                IO.println(response);
            } catch (Exception e) {
                log.error("Error invoking AI agent: {}", e.getMessage());
            }
        }

    } catch (Exception e) {
        IO.println("Connection failed: " + e.getMessage());
        IO.println("Make sure the dice service is running: jbang DiceRollMcpServer.java");
    }
}
