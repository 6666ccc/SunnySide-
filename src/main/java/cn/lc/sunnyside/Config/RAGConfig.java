package cn.lc.sunnyside.Config;

import cn.lc.sunnyside.Memory.ChatMemoryProperties;
import cn.lc.sunnyside.Memory.RedisChatMemoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
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

    /**
     * 构建会话记忆实例。
     * 使用窗口化消息记忆，避免上下文无限增长。
     *
     * @return ChatMemory 实例
     */
    @Bean
    public ChatMemory chatMemory(StringRedisTemplate stringRedisTemplate,
                                 ObjectMapper objectMapper,
                                 ChatMemoryProperties props) {
        RedisChatMemoryRepository repo = new RedisChatMemoryRepository(
                stringRedisTemplate,
                objectMapper,
                props.getRedis().getKeyPrefix(),
                Duration.ofSeconds(props.getRedis().getTtlSeconds()),
                props.getMaxMessages()
        );

        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo)
                .maxMessages(props.getMaxMessages())
                .build();
    }

    /**
     * 应用启动时加载并增量刷新 RAG 知识数据。
     * 当文档内容哈希未变化且未强制重载时，直接跳过向量写入。
     *
     * @param vectorStore 向量存储实例
     * @return 启动回调任务
     */
    @Bean
    public CommandLineRunner loadData(VectorStore vectorStore) {
        return args -> {
            logger.info("正在加载 RAG 知识库数据: {}", ragDataResource.getFilename());
            //读取RAG文档(TextReader是Spring AI提供的读取文本文件的类)
            TextReader textReader = new TextReader(ragDataResource);
            //设置RAG文档的文件名(getCustomMetadata()是TextReader类的方法,用于设置自定义元数据)
            textReader.getCustomMetadata().put("filename", "ragKonloage.txt");
            //读取RAG文档内容(read()是TextReader类的方法,用于读取文本文件内容)
            List<Document> documents = textReader.read();
            //如果RAG文档内容为空,则跳过写入(isEmpty()是List类的方法,用于判断列表是否为空)
            if (documents.isEmpty()) {
                logger.warn("RAG 数据源为空，跳过写入。");
                return;
            }   
            //将RAG文档内容转换为字符串(stream()是Stream类的方法,用于将列表转换为流)
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

            try {
                FilterExpressionBuilder fb = new FilterExpressionBuilder();
                List<Document> oldDocs = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query("*")
                                .topK(10000)
                                .filterExpression(fb.eq("source_file", "ragKonloage.txt").build())
                                .build());
                if (oldDocs != null && !oldDocs.isEmpty()) {
                    List<String> oldIds = oldDocs.stream()
                            .map(Document::getId)
                            .collect(Collectors.toList());
                    vectorStore.delete(oldIds);
                    logger.info("已清除旧向量数据 {} 条，准备重新写入。", oldIds.size());
                }
            } catch (Exception ex) {
                logger.warn("清除旧向量数据失败，将直接执行新增写入（可能存在重复数据）。", ex);
            }

            try {
                //将RAG文档内容写入向量数据库(add()是VectorStore类的方法,用于将文档内容写入向量数据库)
                vectorStore.add(splitDocuments);
                //写入哈希值(writeStoredHash()是RAGConfig类的方法,用于写入哈希值)
                writeStoredHash(hashFilePath, latestHash);
                logger.info("RAG 数据加载完成！共写入 {} 个文档片段。hash={}", splitDocuments.size(), latestHash);
            } catch (Exception ex) {
                logger.error("RAG 向量数据写入失败！本次 hash 不会更新，下次启动将自动重试。hash={}", latestHash, ex);
            }
        };
    }

    /**
     * 读取历史知识文件哈希值。
     *
     * @param hashFilePath 哈希文件路径
     * @return 已保存哈希；文件不存在或读取失败时返回空字符串
     */
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

    /**
     * 写入最新知识文件哈希值。
     *
     * @param hashFilePath 哈希文件路径
     * @param hash 最新哈希
     * @throws Exception 文件系统异常
     */
    private void writeStoredHash(Path hashFilePath, String hash) throws Exception {
        Path parent = hashFilePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(hashFilePath, hash, StandardCharsets.UTF_8);
    }

    /**
     * 计算文本内容的 SHA-256 哈希。
     *
     * @param content 输入文本
     * @return 16进制哈希字符串
     * @throws Exception 摘要计算异常
     */
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
