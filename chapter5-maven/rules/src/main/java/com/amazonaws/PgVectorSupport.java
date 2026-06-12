package com.amazonaws;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

/// Postgres + pgvector for Spring AI RAG. Requires `docker compose up -d`.
public final class PgVectorSupport {

    private PgVectorSupport() {
    }

    public static String jdbcUrl() {
        var host = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
        var port = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
        var db = System.getenv().getOrDefault("POSTGRES_DB", "spring_ai");
        return "jdbc:postgresql://%s:%s/%s".formatted(host, port, db);
    }

    public static HikariDataSource dataSource() {
        var config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl());
        config.setUsername(System.getenv().getOrDefault("POSTGRES_USER", "spring_ai"));
        config.setPassword(System.getenv().getOrDefault("POSTGRES_PASSWORD", "spring_ai"));
        config.setMaximumPoolSize(4);
        return new HikariDataSource(config);
    }

    public static PgVectorStore vectorStore(EmbeddingModel embeddingModel) {
        var jdbc = new JdbcTemplate(dataSource());
        return PgVectorStore.builder(jdbc, embeddingModel)
                .dimensions(Integer.parseInt(
                        System.getenv().getOrDefault("DASHSCOPE_EMBEDDING_DIMENSIONS", "1024")))
                .initializeSchema(true)
                .build();
    }
}
