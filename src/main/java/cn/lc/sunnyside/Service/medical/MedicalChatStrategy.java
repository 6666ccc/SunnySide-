package cn.lc.sunnyside.Service.medical;

/**
 * 医疗对话策略：负责调用具体能力（loop/workflow）并返回可用性信息。
 */
public interface MedicalChatStrategy {

    MedicalChatAttempt tryReply(MedicalChatContext ctx);
}

