package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DTO.ChatReply;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 对话服务抽象。
 * 定义老人端、家属端、流式与多模态四类核心会话能力。
 */
public interface AIService {
    /**
     * 老人端文本对话。
     *
     * @param userInput 用户文本输入
     * @param conversationId 对话会话ID
     * @return 单轮回复结果
     */
    ChatReply.ChatReplyRecord elderChat(String userInput, String conversationId);

    /**
     * 家属端文本对话。
     *
     * @param userInput 用户文本输入
     * @param conversationId 对话会话ID
     * @return 单轮回复结果
     */
    ChatReply.ChatReplyRecord relativesChat(String userInput, String conversationId);

    /**
     * 流式文本对话。
     *
     * @param userInput 用户文本输入
     * @param conversationId 对话会话ID
     * @return 连续输出的回复流
     */
    Flux<String> streamChat(String userInput, String conversationId);

    /**
     * 多模态对话。
     *
     * @param userInput 用户文本输入
     * @param mediaFiles 上传的媒体文件
     * @param conversationId 对话会话ID
     * @return 单轮回复结果
     */
    ChatReply.ChatReplyRecord multimodalChat(String userInput, List<MultipartFile> mediaFiles, String conversationId);
}
