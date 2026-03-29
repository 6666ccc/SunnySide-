package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.VitalSigns;
import java.time.LocalDate;
import java.util.List;

public interface VitalSignsService {

    List<VitalSigns> getLatestVitalSigns(Long patientId, int limit);

    List<VitalSigns> queryByDateRange(Long patientId, LocalDate startDate, LocalDate endDate);

    String formatVitalSignsSummary(Long patientId);
}
