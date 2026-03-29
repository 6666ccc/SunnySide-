package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.DietaryAdvice;
import java.time.LocalDate;
import java.util.List;

public interface DietaryAdviceService {

    List<DietaryAdvice> getDietaryAdvice(Long patientId, LocalDate date);

    List<DietaryAdvice> getDietaryAdviceByType(Long patientId, LocalDate date, String mealType);
}
