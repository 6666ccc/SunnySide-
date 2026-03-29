package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.TreatmentPlan;
import cn.lc.sunnyside.Service.TreatmentPlanService;
import cn.lc.sunnyside.mapper.TreatmentPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TreatmentPlanServiceImpl implements TreatmentPlanService {

    private final TreatmentPlanMapper treatmentPlanMapper;

    @Override
    public List<TreatmentPlan> getTreatmentPlans(Long patientId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return treatmentPlanMapper.selectByPatientIdAndDate(patientId, targetDate);
    }
}
