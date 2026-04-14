package com.example.project.ai.rag;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;



/**
 * 这个类是用于配置RAG的模块化顾问
 */
@Configuration
public class RagModularAdvisorConfig {

    //将向量库文档检索器注入到Spring容器中
    @Bean
    public VectorStoreDocumentRetriever vectorStoreDocumentRetriever(VectorStore vectorStore) {
        return VectorStoreDocumentRetriever.builder()
            .vectorStore(vectorStore)
            .similarityThreshold(0.35)
            .topK(5)
            // .filterExpression(...) // 可选：元数据过滤
            .build();
    }

    //将检索增强顾问注入到Spring容器中
    @Bean
    public Advisor retrievalAugmentationAdvisor(
            VectorStoreDocumentRetriever vectorStoreDocumentRetriever
            // , ChatClient.Builder rewriteChatClientBuilder // 若启用 RewriteQueryTransformer
    ) {
        var builder = RetrievalAugmentationAdvisor.builder()
            .documentRetriever(vectorStoreDocumentRetriever)
            .queryAugmenter(ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)//允许空上下文
                .build());
        return builder.build();
    }

    // 若使用 RewriteQueryTransformer，通常再提供一个低温度、专用于改写的 ChatClient.Builder
    // @Bean
    // public ChatClient.Builder ragRewriteChatClientBuilder(ChatModel chatModel) {
    //     return ChatClient.builder(chatModel); // 并在 ChatModel 或此处配置低 temperature
    // }
}