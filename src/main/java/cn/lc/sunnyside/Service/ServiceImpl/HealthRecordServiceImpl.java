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

    @Override
    public String queryElderHealth(String familyPhone, Long elderId, LocalDate startDate, LocalDate endDate) {
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
        if (normalizedStart.isAfter(normalizedEnd)) {
            LocalDate temp = normalizedStart;
            normalizedStart = normalizedEnd;
            normalizedEnd = temp;
        }

        List<HealthRecord> records = healthRecordMapper.selectByElderIdAndDateRange(elderId, normalizedStart,
                normalizedEnd);
        if (records == null || records.isEmpty()) {
            return "该时间范围没有健康记录。";
        }

        HealthRecord latest = records.stream()
                .max(Comparator.comparing(HealthRecord::getRecordDate).thenComparing(HealthRecord::getRecordTime))
                .orElse(records.get(0));
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

    private String averageInt(List<Integer> values) {
        OptionalDouble optionalDouble = values.stream().filter(v -> v != null).mapToInt(Integer::intValue).average();
        return optionalDouble.isPresent() ? String.format("%.1f", optionalDouble.getAsDouble()) : "-";
    }

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

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }
}
