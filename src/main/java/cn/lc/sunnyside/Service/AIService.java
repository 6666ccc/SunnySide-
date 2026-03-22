package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DTO.ChatReply;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AIService {
    ChatReply.ChatReplyRecord elderChat(String userInput, String conversationId);

    ChatReply.ChatReplyRecord relativesChat(String userInput, String conversationId);

    Flux<String> streamChat(String userInput, String conversationId);

    ChatReply.ChatReplyRecord multimodalChat(String userInput, List<MultipartFile> mediaFiles, String conversationId);
}
