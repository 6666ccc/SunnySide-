package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.TreatmentPlan;
import java.time.LocalDate;
import java.util.List;

public interface TreatmentPlanService {

    List<TreatmentPlan> getTreatmentPlans(Long patientId, LocalDate date);
}
