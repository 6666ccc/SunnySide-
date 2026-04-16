package com.example.project.ai.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 启动灌库策略：默认按内容 hash 增量，变更时先按逻辑文档 {@code doc_key} 删除再写入。
 */
@ConfigurationProperties(prefix = "app.ai.rag-ingest")
public class RagIngestProperties {

    /**
     * 是否在容器启动时执行灌库。
     */
    private boolean enabled = true;

    /**
     * 为同一逻辑语料在 Qdrant payload 中使用的稳定键，用于按文档删除旧分片。
     */
    private String docKey = "kb:rag/RAG.txt";

    /**
     * true：忽略本地 hash 记录，每次启动都先删后灌（仍比「不清库」少重复，但会重复调用 Embedding）。
     */
    private boolean force = false;

    /**
     * 记录已成功灌入的内容 SHA-256（hex）。默认：{@code java.io.tmpdir/sunnyside-rag/ingest-state.properties}。
     * 若 Qdrant 被手工清空而此处仍有旧 hash，可删此文件或设 {@link #force} 强制重灌。
     */
    private String statePath;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDocKey() {
        return docKey;
    }

    public void setDocKey(String docKey) {
        this.docKey = docKey;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public String getStatePath() {
        return statePath;
    }

    public void setStatePath(String statePath) {
        this.statePath = statePath;
    }
}
