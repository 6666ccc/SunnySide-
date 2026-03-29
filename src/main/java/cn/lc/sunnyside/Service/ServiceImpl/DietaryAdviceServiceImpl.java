package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.DietaryAdvice;
import cn.lc.sunnyside.Service.DietaryAdviceService;
import cn.lc.sunnyside.mapper.DietaryAdviceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * @author lc
 */
@Service
@RequiredArgsConstructor
public class DietaryAdviceServiceImpl implements DietaryAdviceService {

    private final DietaryAdviceMapper dietaryAdviceMapper;

    @Override
    public List<DietaryAdvice> getDietaryAdvice(Long patientId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return dietaryAdviceMapper.selectByPatientIdAndDate(patientId, targetDate);
    }

    @Override
    public List<DietaryAdvice> getDietaryAdviceByType(Long patientId, LocalDate date, String mealType) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return dietaryAdviceMapper.selectByPatientIdAndDateAndType(patientId, targetDate, mealType);
    }
}
