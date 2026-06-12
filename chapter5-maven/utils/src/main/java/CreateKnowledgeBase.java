import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.util.List;

/// Creates a D&D rules knowledge base from the Basic Rules PDF.
/// Embeddings: DashScope OpenAI-compatible API. Storage: Postgres pgvector.
///
/// Prerequisites:
///   1. docker compose up -d
///   2. DASHSCOPE_API_KEY in environment (see .env.example)
///   3. Download "DnD_BasicRules_2018.pdf" and place it in this directory
///
/// Usage: jbang CreateKnowledgeBase.java

private static final Logger log = LoggerFactory.getLogger("CreateKnowledgeBase");
private static final String PDF_FILE = "DnD_BasicRules_2018.pdf";

void main(String[] args) {
    var pdfFile = new File(PDF_FILE);
    if (!pdfFile.exists()) {
        log.error("PDF file not found: {}", pdfFile.getAbsolutePath());
        log.error("Download the D&D Basic Rules PDF and place it in this directory.");
        return;
    }
    log.info("Found PDF: {} ({} bytes)", PDF_FILE, pdfFile.length());

    var apiKey = System.getenv("DASHSCOPE_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
        log.error("Set DASHSCOPE_API_KEY — create one at https://dashscope.console.aliyun.com/apiKey");
        return;
    }
    var baseUrl = System.getenv().get("DASHSCOPE_BASE_URL");
    var embeddingModelName = System.getenv().getOrDefault("DASHSCOPE_EMBEDDING_MODEL", "text-embedding-v3");
    var embeddingDimensions = Integer.parseInt(
            System.getenv().getOrDefault("DASHSCOPE_EMBEDDING_DIMENSIONS", "1024"));

    log.info("Embedding model: {} ({} dims) @ {}", embeddingModelName, embeddingDimensions, baseUrl);

    log.info("Reading PDF...");
    var pdfReader = new PagePdfDocumentReader(new FileSystemResource(pdfFile));
    List<Document> pages = pdfReader.get();
    log.info("Extracted {} pages from PDF", pages.size());

    log.info("Splitting into chunks...");
    var splitter = new TokenTextSplitter();
    List<Document> chunks = splitter.apply(pages).stream()
            .filter(doc -> doc.getText().length() >= 50)
            .toList();
    log.info("Created {} chunks after filtering", chunks.size());

    log.info("Connecting to Postgres {} ...", PgVectorSupport.jdbcUrl());
    var client = new OpenAIClientImpl(ClientOptions.builder()
            .httpClient(SpringAiOpenAiHttpClient.builder().build())
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .build());
    EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
            .openAiClient(client)
            .metadataMode(MetadataMode.EMBED)
            .options(OpenAiEmbeddingOptions.builder()
                    .model(embeddingModelName)
                    .dimensions(embeddingDimensions)
                    .build())
            .build();
    var vectorStore = PgVectorSupport.vectorStore(embeddingModel);

    log.info("Computing embeddings and writing to pgvector (this may take a while)...");
    int batchSize = 100;
    for (int i = 0; i < chunks.size(); i += batchSize) {
        int end = Math.min(i + batchSize, chunks.size());
        var batch = chunks.subList(i, end);
        vectorStore.add(batch);
        log.info("  Added batch {}/{} ({} documents)",
                (i / batchSize) + 1,
                (int) Math.ceil((double) chunks.size() / batchSize),
                batch.size());
    }

    log.info("Done! Start RulesAgent — it reads from the same pgvector table.");
}
