package com.example.project.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class RagIngestService {

    @Autowired
    private VectorStore vectorStore;

    @Value("classpath:RAG/RAG.txt")
    private Resource ragTextResource;


    //这个是元数据，用于记录文档的来源
    private static final String META_SOURCE = "source";

    /**
     * 读取 classpath:RAG/RAG.txt，按 Token 切分后写入向量库（内部会 Embedding 并 upsert 到 Qdrant）。
     */
    @PostConstruct//在Spring容器启动时执行
    public void ingest() {
        String text;
        try {
            text = readResourceAsUtf8(ragTextResource);
        } catch (IOException e) {
            throw new UncheckedIOException("无法读取 RAG/RAG.txt", e);
        }
        if (!StringUtils.hasText(text)) {
            return;
        }
        Map<String, Object> metadata = Map.of(
                META_SOURCE, "RAG/RAG.txt",
                "type", "kb");
        Document root = new Document(text.strip(), metadata);
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.split(root);
        vectorStore.add(chunks);
    }

    private static String readResourceAsUtf8(Resource resource) throws IOException {
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}