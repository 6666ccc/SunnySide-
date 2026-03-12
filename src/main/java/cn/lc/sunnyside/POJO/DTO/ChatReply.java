package cn.lc.sunnyside.POJO.DTO;

import java.util.List;

public class ChatReply {
    public record ChatReplyRecord(String answer, List<String> suggestedQuestions) {
    }

}
