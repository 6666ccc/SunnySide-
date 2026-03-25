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
     * @param startDate   开始日期（可选，默认当天）
     * @param endDate     结束日期（可选，默认当天）
     * @return 健康记录详情（包含最近记录与各指标均值）或错误提示信息
     */
    @Override
    public String queryElderHealth(String familyPhone, Long elderId, LocalDate startDate, LocalDate endDate) {
        // 根据家属账号查询家属信息，验证家属存在性
        FamilyUser familyUser = familyUserMapper.selectByPhone(normalizePhone(familyPhone));
        if (familyUser == null || elderId == null) {
            return "查询失败，家属账号或老人信息无效。";
        }

        // 校验家属和老人之间是否存在绑定关系，确保数据访问权限
        Integer count = familyElderRelationMapper.existsRelation(familyUser.getId(), elderId);
        if (count == null || count <= 0) {
            return "查询失败，家属与老人不存在绑定关系。";
        }

        // 处理并规范化查询时间范围，若未提供则默认查询当天的记录
        LocalDate normalizedStart = LocalDate.now();
        if (startDate != null) {
            normalizedStart = startDate;
        }
        LocalDate normalizedEnd = normalizedStart;
        if (endDate != null) {
            normalizedEnd = endDate;
        }
        // 确保开始日期不晚于结束日期，自动纠正参数顺序
        if (normalizedStart.isAfter(normalizedEnd)) {
            LocalDate temp = normalizedStart;
            normalizedStart = normalizedEnd;
            normalizedEnd = temp;
        }

        // 根据老人ID和时间范围查询健康记录
        List<HealthRecord> records = healthRecordMapper.selectByElderIdAndDateRange(elderId, normalizedStart,
                normalizedEnd);
        if (records == null || records.isEmpty()) {
            return "该时间范围没有健康记录。";
        }

        // 查找最新的一条健康记录，按照记录日期和时间进行排序
        HealthRecord latest = records.stream()
                .max(Comparator.comparing(HealthRecord::getRecordDate).thenComparing(HealthRecord::getRecordTime))
                .orElse(records.get(0));

        // 计算各项生理指标在查询时间段内的平均值
        String avgHeartRate = averageInt(records.stream().map(HealthRecord::getHeartRate).toList());
        String avgSystolic = averageInt(records.stream().map(HealthRecord::getSystolicBp).toList());
        String avgDiastolic = averageInt(records.stream().map(HealthRecord::getDiastolicBp).toList());
        String avgBloodSugar = averageDecimal(records.stream().map(HealthRecord::getBloodSugar).toList());

        // 拼接返回结果
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
        if (optionalDouble.isPresent()) {
            return String.format("%.1f", optionalDouble.getAsDouble());
        }
        return "-";
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

    /**
     * 将对象转换为文本，处理空值情况
     *
     * @param value 任意对象值
     * @return 转换后的字符串，如果对象为空则返回 "-"
     */
    private String toText(Object value) {
        if (value == null) {
            return "-";
        }
        return String.valueOf(value);
    }

    /**
     * 归一化手机号，移除前导空格
     *
     * @param phone 手机号
     * @return 归一化后的手机号
     */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        return phone.trim();
    }
}
