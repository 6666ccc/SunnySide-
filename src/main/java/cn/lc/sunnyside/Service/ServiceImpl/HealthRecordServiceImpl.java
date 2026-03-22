package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.FamilyUser;
import cn.lc.sunnyside.POJO.DO.HealthRecord;
import cn.lc.sunnyside.Service.HealthRecordService;
import cn.lc.sunnyside.mapper.FamilyElderRelationMapper;
import cn.lc.sunnyside.mapper.FamilyUserMapper;
import cn.lc.sunnyside.mapper.HealthRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

@Service
@RequiredArgsConstructor
public class HealthRecordServiceImpl implements HealthRecordService {

    private final FamilyUserMapper familyUserMapper;
    private final FamilyElderRelationMapper familyElderRelationMapper;
    private final HealthRecordMapper healthRecordMapper;

    /**
     * 查询老人在指定时间范围内的健康记录
     *
     * @param familyPhone 家属账号
     * @param elderId     老人ID
     * @param startDate   开始日期（可选）
     * @param endDate     结束日期（可选）
     * @return 健康记录详情或错误信息
     */
    @Override
    public String queryElderHealth(String familyPhone, Long elderId, LocalDate startDate, LocalDate endDate) {
        // 根据家属账号查询家属信息
        FamilyUser familyUser = familyUserMapper.selectByPhone(normalizePhone(familyPhone));
        if (familyUser == null || elderId == null) {
            return "查询失败，家属账号或老人信息无效。";
        }
        Integer count = familyElderRelationMapper.existsRelation(familyUser.getId(), elderId);
        if (count == null || count <= 0) {
            return "查询失败，家属与老人不存在绑定关系。";
        }

        LocalDate normalizedStart = startDate == null ? LocalDate.now() : startDate;
        LocalDate normalizedEnd = endDate == null ? normalizedStart : endDate;
        // 确保开始日期不晚于结束日期
        if (normalizedStart.isAfter(normalizedEnd)) {
            LocalDate temp = normalizedStart;
            normalizedStart = normalizedEnd;
            normalizedEnd = temp;
        }

        // 根据老人ID和时间范围查询健康记录
        List<HealthRecord> records = healthRecordMapper.selectByElderIdAndDateRange(elderId, normalizedStart,normalizedEnd);
        if (records == null || records.isEmpty()) {
            return "该时间范围没有健康记录。";
        }
        
        // 查找最新的健康记录
        HealthRecord latest = records.stream()
                .max(Comparator.comparing(HealthRecord::getRecordDate).thenComparing(HealthRecord::getRecordTime))
                .orElse(records.get(0));
        // 计算平均值
        String avgHeartRate = averageInt(records.stream().map(HealthRecord::getHeartRate).toList());
        String avgSystolic = averageInt(records.stream().map(HealthRecord::getSystolicBp).toList());
        String avgDiastolic = averageInt(records.stream().map(HealthRecord::getDiastolicBp).toList());
        String avgBloodSugar = averageDecimal(records.stream().map(HealthRecord::getBloodSugar).toList());

        return "时间范围:" + normalizedStart + "至" + normalizedEnd +
                "\n记录条数:" + records.size() +
                "\n最近一次: 日期" + latest.getRecordDate() + " 时间" + latest.getRecordTime() +
                " 血压" + toText(latest.getSystolicBp()) + "/" + toText(latest.getDiastolicBp()) +
                " 心率" + toText(latest.getHeartRate()) +
                " 血糖" + toText(latest.getBloodSugar()) +
                " 体温" + toText(latest.getTemperature()) +
                "\n均值: 血压" + avgSystolic + "/" + avgDiastolic + " 心率" + avgHeartRate + " 血糖" + avgBloodSugar;
    }

    /**
     * 计算整数列表的平均值，保留一位小数
     *
     * @param values 整数列表
     * @return 平均值字符串，或"-"如果列表为空或包含null值
     */
    private String averageInt(List<Integer> values) {
        OptionalDouble optionalDouble = values.stream().filter(v -> v != null).mapToInt(Integer::intValue).average();
        return optionalDouble.isPresent() ? String.format("%.1f", optionalDouble.getAsDouble()) : "-";
    }

    /**
     * 计算BigDecimal列表的平均值，保留两位小数
     *
     * @param values BigDecimal列表
     * @return 平均值字符串，或"-"如果列表为空或包含null值
     */
    private String averageDecimal(List<BigDecimal> values) {
        List<BigDecimal> valid = values.stream().filter(v -> v != null).toList();
        if (valid.isEmpty()) {
            return "-";
        }
        BigDecimal sum = valid.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(valid.size()), 2, RoundingMode.HALF_UP).toPlainString();
    }

    private String toText(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    /**
     * 归一化手机号，移除前导空格
     *
     * @param phone 手机号
     * @return 归一化后的手机号
     */
    private String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }
}
