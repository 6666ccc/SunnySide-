package com.example.project.ai.rag;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 读取 classpath 语料、切分后写入向量库。
 * <p>
 * 策略：对整篇文本做 SHA-256，与本地状态比对；未变更则跳过（无 Embedding、无写入）。
 * 变更或强制重灌时：先按元数据 {@value #META_DOC_KEY} 删除该逻辑文档在库中的旧分片，再写入，避免 Qdrant 追加导致重复检索。
 * <p>
 * 注意：若历史向量无 {@value #META_DOC_KEY} 字段，按 key 删除不会清理旧数据，需自行清空 collection 或迁移。
 */
@Component
public class RagIngestService {

    private static final Logger log = LoggerFactory.getLogger(RagIngestService.class);

    static final String META_SOURCE = "source";
    static final String META_DOC_KEY = "doc_key";
    static final String META_CONTENT_HASH = "content_hash";

    private final VectorStore vectorStore;
    private final RagIngestProperties ingestProperties;

    @Value("classpath:RAG/RAG.txt")
    private Resource ragTextResource;

    public RagIngestService(VectorStore vectorStore, RagIngestProperties ingestProperties) {
        this.vectorStore = vectorStore;
        this.ingestProperties = ingestProperties;
    }

    @PostConstruct
    public void ingest() {
        if (!ingestProperties.isEnabled()) {
            log.info("RAG 灌库已关闭（app.ai.rag-ingest.enabled=false）");
            return;
        }
        String text;
        try {
            text = readResourceAsUtf8(ragTextResource);
        } catch (IOException e) {
            throw new UncheckedIOException("无法读取 RAG/RAG.txt", e);
        }
        if (!StringUtils.hasText(text)) {
            log.warn("RAG/RAG.txt 为空，跳过灌库");
            return;
        }
        String stripped = text.strip();
        String contentHash = sha256Hex(stripped);
        String docKey = ingestProperties.getDocKey();

        if (!ingestProperties.isForce()) {
            String last = readStoredHash(docKey);
            if (contentHash.equals(last)) {
                log.info("RAG 语料未变更（doc_key={}），跳过灌库与 Embedding", docKey);
                return;
            }
        }

        Map<String, Object> metadata = Map.of(
                META_SOURCE, "RAG/RAG.txt",
                "type", "kb",
                META_DOC_KEY, docKey,
                META_CONTENT_HASH, contentHash);
        Document root = new Document(stripped, metadata);
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.split(root);

        try {
            vectorStore.delete(new FilterExpressionBuilder().eq(META_DOC_KEY, docKey).build());
        } catch (RuntimeException e) {
            log.warn("按 doc_key 删除旧向量失败（可能为首次灌库或 Qdrant 无匹配 payload），继续写入: {}", e.toString());
        }

        vectorStore.add(chunks);
        storeHash(docKey, contentHash);
        log.info("RAG 灌库完成：doc_key={}，分片数={}，content_hash={}", docKey, chunks.size(), contentHash);
    }

    private String readStoredHash(String docKey) {
        Path path = stateFilePath();
        if (!Files.isRegularFile(path)) {
            return null;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
        } catch (IOException e) {
            log.warn("读取 RAG 灌库状态文件失败，将视为无记录: {}", path, e);
            return null;
        }
        return p.getProperty(docKey);
    }

    private void storeHash(String docKey, String contentHash) {
        Path path = stateFilePath();
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            log.warn("创建 RAG 状态目录失败，无法持久化 hash: {}", path.getParent(), e);
            return;
        }
        Properties p = new Properties();
        if (Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                p.load(in);
            } catch (IOException ignored) {
                // 覆盖写入
            }
        }
        p.setProperty(docKey, contentHash);
        try (OutputStream out = Files.newOutputStream(path)) {
            p.store(out, "SunnySide RAG ingest content hashes (SHA-256 hex)");
        } catch (IOException e) {
            log.warn("写入 RAG 灌库状态文件失败: {}", path, e);
        }
    }

    private Path stateFilePath() {
        if (StringUtils.hasText(ingestProperties.getStatePath())) {
            return Path.of(ingestProperties.getStatePath());
        }
        return Path.of(System.getProperty("java.io.tmpdir"), "sunnyside-rag", "ingest-state.properties");
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String readResourceAsUtf8(Resource resource) throws IOException {
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
