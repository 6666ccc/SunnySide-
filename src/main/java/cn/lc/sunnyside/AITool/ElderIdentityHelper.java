package cn.lc.sunnyside.AITool;

import cn.lc.sunnyside.POJO.DO.ElderlyUser;
import cn.lc.sunnyside.Service.ElderlyUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ElderIdentityHelper {

    private final ElderlyUserService elderlyUserService;

    /**
     * 解析老人ID，优先使用直接提供的ID，如果没有则根据身份线索查找。
     * 如果根据身份线索找到唯一匹配的老人，则返回该老人的ID；如果没有找到或找到多个，则返回null。
     */
    public Long resolveElderId(Long elderlyId, String elderRef) {
        if (elderlyId != null) {
            return elderlyId;
        }
        // 根据身份线索查找匹配的老人，如果找到唯一一个则返回其ID
        List<ElderlyUser> candidates = findCandidates(elderRef);
        if (candidates.size() == 1) {
            return candidates.get(0).getId();
        }
        return null;
    }

    /**
     * 根据身份线索查找匹配的老人，支持姓名、手机号后4位等模糊匹配。
     */
    public List<ElderlyUser> findCandidates(String elderRef) {
        if (elderRef == null || elderRef.isBlank()) {
            return List.of();
        }
        return elderlyUserService.findByRef(elderRef).stream()
                .sorted(Comparator.comparing(ElderlyUser::getId))
                .toList();
    }

    /**
     * 当无法唯一识别老人时，生成更可读的错误提示文案。
     *
     * @param elderRef 用户提供的老人身份线索
     * @return 无匹配或多匹配时的提示文本
     */
    public String unresolvedMessage(String elderRef) {
        List<ElderlyUser> candidates = findCandidates(elderRef);
        if (candidates.isEmpty()) {
            return "未找到匹配老人，请提供老人姓名或手机号后4位。";
        }
        return "匹配到多位老人，请先确认具体对象: " + candidates.stream()
                .map(this::formatElderBrief)
                .collect(Collectors.joining("; "));
    }

    /**
     * 格式化老人简要信息，包含ID、姓名和手机号后4位（如果有）。
     */
    public String formatElderBrief(ElderlyUser elder) {
        String phoneTail = "";
        if (elder.getPhone() != null && elder.getPhone().length() >= 4) {
            phoneTail = " 手机尾号:" + elder.getPhone().substring(elder.getPhone().length() - 4);
        }
        return "ID:" + elder.getId() + " 姓名:" + elder.getFullName() + phoneTail;
    }

    /**
     * 标准化字符串，将其转换为大写并去除首尾空格。如果字符串为空或仅包含空格，则返回null。
     */
    public String normalizeOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim().toUpperCase();
    }

    /**
     * 标准化字符串，将其转换为大写并去除首尾空格。如果字符串为空或仅包含空格，则返回空字符串。
     */
    public String normalizeRequired(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toUpperCase();
    }

    /**
     * 解析日期字符串为LocalDate对象，支持"yyyy-MM-dd"格式。如果字符串为空或仅包含空格，则返回null。
     */
    public LocalDate parseDateOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDate.parse(text.trim());
    }

    /**
     * 解析日期时间字符串为LocalDateTime对象，支持"yyyy-MM-dd HH:mm:ss"格式。如果字符串为空或仅包含空格，则返回null。
     */
    public LocalDateTime parseDateTimeOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(text.trim());
    }
}
