# Once Upon Spring AI

基于 [aws-samples/sample-once-upon-spring-ai](https://github.com/aws-samples/sample-once-upon-spring-ai) 改写：**DashScope OpenAI 兼容模式** + **Postgres pgvector**，无 Amazon Bedrock。

## 技术栈

| 组件              | 版本                                  |
|-----------------|-------------------------------------|
| Java            | 25                                  |
| Spring Boot     | 4.1.0                               |
| Spring AI       | 2.0.0-RC2                           |
| LLM / Embedding | 阿里云 DashScope（compatible-mode/v1）   |
| 向量库             | Postgres + pgvector（docker compose） |

## 快速开始

```bash
cp .env.example .env   # 填入 DASHSCOPE_API_KEY
docker compose up -d

# Ch1 smoke test
export $(grep -v '^#' .env | xargs)
jbang chapter1/DungeonMasterSimple.java

# Ch5 RAG：下载 DnD_BasicRules_2018.pdf 到 chapter5/utils/
cd chapter5/utils && jbang CreateKnowledgeBase.java
cd ../agents/rules && jbang RulesAgent.java
```

## 环境变量

见 [`.env.example`](.env.example)。核心：

- `DASHSCOPE_API_KEY` — [百炼控制台](https://dashscope.console.aliyun.com/apiKey)
- `DASHSCOPE_BASE_URL` — 默认 `https://dashscope.aliyuncs.com/compatible-mode/v1`
- `DASHSCOPE_CHAT_MODEL` — 默认 `qwen3.6-plus`
- `DASHSCOPE_EMBEDDING_MODEL` — 默认 `text-embedding-v3`（1024 维）

## 目录

- `shared/` — `PgVectorSupport`（JBang `//SOURCES`）
- `chapterN/` — JBang workshop 源码
- `chapterN-maven/` — Maven workshop 源码
- `docker/` — Postgres init SQL

## 与 upstream 差异

- Bedrock → DashScope OpenAI 兼容 API
- `SimpleVectorStore` + json → **PgVector**
- compose 仅 Postgres（无 Ollama / 无 Bedrock）
- Spring AI **2.0.0-RC2** + Boot **4.1.0**
