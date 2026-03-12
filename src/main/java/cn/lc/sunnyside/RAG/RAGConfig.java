package cn.lc.sunnyside.RAG;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class RAGConfig {

    private static final Logger logger = LoggerFactory.getLogger(RAGConfig.class);

    @Value("classpath:rag/ragKonloage.txt")
    private Resource ragDataResource;

    @Value("${app.rag.hash-file:target/rag/ragKonloage.sha256}")
    private String ragHashFile;

    @Value("${app.rag.force-reload:false}")
    private boolean forceReload;

    @Bean
    public CommandLineRunner loadData(VectorStore vectorStore) {
        return args -> {
            logger.info("正在加载 RAG 知识库数据: {}", ragDataResource.getFilename());
            TextReader textReader = new TextReader(ragDataResource);
            textReader.getCustomMetadata().put("filename", "ragKonloage.txt");
            List<Document> documents = textReader.read();
            if (documents.isEmpty()) {
                logger.warn("RAG 数据源为空，跳过写入。");
                return;
            }

            String rawContent = documents.stream().map(Document::getText).collect(Collectors.joining("\n"));
            String latestHash = sha256(rawContent);
            Path hashFilePath = Path.of(ragHashFile);
            String storedHash = readStoredHash(hashFilePath);

            if (!forceReload && latestHash.equals(storedHash)) {
                logger.info("RAG 文档未变化，跳过向量写入。hash={}", latestHash);
                return;
            }

            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocuments = splitter.apply(documents);
            splitDocuments.forEach(document -> {
                document.getMetadata().put("source_file", "ragKonloage.txt");
                document.getMetadata().put("source_hash", latestHash);
            });

            // Step 1: 写入前先删除同 source_file 的旧向量数据，防止因重复写入累积脏数据
            try {
                FilterExpressionBuilder fb = new FilterExpressionBuilder();
                List<Document> oldDocs = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query("*")
                                .topK(10000)
                                .filterExpression(fb.eq("source_file", "ragKonloage.txt").build())
                                .build()
                );
                if (oldDocs != null && !oldDocs.isEmpty()) {
                    List<String> oldIds = oldDocs.stream()
                            .map(Document::getId)
                            .collect(Collectors.toList());
                    vectorStore.delete(oldIds);
                    logger.info("已清除旧向量数据 {} 条，准备重新写入。", oldIds.size());
                }
            } catch (Exception ex) {
                // 删除旧数据失败不应阻断整个流程，记录警告后继续写入
                logger.warn("清除旧向量数据失败，将直接执行新增写入（可能存在重复数据）。", ex);
            }

            // Step 2: 写入新向量数据，仅在成功后才持久化 hash，保证原子性
            try {
                vectorStore.add(splitDocuments);
                // ✅ 仅写入成功后才更新 hash，避免写入中途失败导致 hash 与向量库不一致
                writeStoredHash(hashFilePath, latestHash);
                logger.info("RAG 数据加载完成！共写入 {} 个文档片段。hash={}", splitDocuments.size(), latestHash);
            } catch (Exception ex) {
                // ❌ 写入失败：不更新 hash，下次启动将强制重试
                logger.error("RAG 向量数据写入失败！本次 hash 不会更新，下次启动将自动重试。hash={}", latestHash, ex);
            }
        };
    }

    private String readStoredHash(Path hashFilePath) {
        try {
            if (!Files.exists(hashFilePath)) {
                return "";
            }
            return Files.readString(hashFilePath, StandardCharsets.UTF_8).trim();
        } catch (Exception ex) {
            logger.warn("读取 RAG 哈希文件失败，将执行写入。path={}", hashFilePath, ex);
            return "";
        }
    }

    private void writeStoredHash(Path hashFilePath, String hash) throws Exception {
        Path parent = hashFilePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(hashFilePath, hash, StandardCharsets.UTF_8);
    }

    private String sha256(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hashed.length * 2);
        for (byte b : hashed) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
