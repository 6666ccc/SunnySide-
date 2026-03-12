package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.MedicalDuty;
import cn.lc.sunnyside.Service.MedicalDutyService;
import cn.lc.sunnyside.mapper.MedicalDutyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalDutyServiceImpl implements MedicalDutyService {

    private final MedicalDutyMapper medicalDutyMapper;

    @Override
    public List<MedicalDuty> getOnDutyStaff() {
        return medicalDutyMapper.selectOnDuty(LocalDate.now());
    }
}
