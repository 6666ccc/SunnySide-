package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.VitalSigns;
import cn.lc.sunnyside.Service.VitalSignsService;
import cn.lc.sunnyside.mapper.VitalSignsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VitalSignsServiceImpl implements VitalSignsService {

    private final VitalSignsMapper vitalSignsMapper;

    @Override
    public List<VitalSigns> getLatestVitalSigns(Long patientId, int limit) {
        return vitalSignsMapper.selectLatestByPatientId(patientId, limit);
    }

    @Override
    public List<VitalSigns> queryByDateRange(Long patientId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start;
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        return vitalSignsMapper.selectByPatientIdAndDateRange(patientId, start, end);
    }

    @Override
    public String formatVitalSignsSummary(Long patientId) {
        List<VitalSigns> records = vitalSignsMapper.selectLatestByPatientId(patientId, 1);
        if (records == null || records.isEmpty()) {
            return "暂无该患者的生命体征记录。";
        }
        VitalSigns latest = records.get(0);
        return "最近体征 (" + latest.getRecordDate() + " " + latest.getRecordTime() + ")"
                + " 血压:" + toText(latest.getSystolicBp()) + "/" + toText(latest.getDiastolicBp()) + "mmHg"
                + " 心率:" + toText(latest.getHeartRate()) + "bpm"
                + " 体温:" + toText(latest.getTemperature()) + "℃"
                + " 血氧:" + toText(latest.getBloodOxygen()) + "%"
                + " 血糖:" + toText(latest.getBloodSugar()) + "mmol/L"
                + " 记录人:" + toText(latest.getRecordedBy());
    }

    private String toText(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
