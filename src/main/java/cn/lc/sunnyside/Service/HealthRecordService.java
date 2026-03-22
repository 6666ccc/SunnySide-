package cn.lc.sunnyside.Service;

import java.time.LocalDate;

public interface HealthRecordService {

    String queryElderHealth(String familyPhone, Long elderId, LocalDate startDate, LocalDate endDate);
}
