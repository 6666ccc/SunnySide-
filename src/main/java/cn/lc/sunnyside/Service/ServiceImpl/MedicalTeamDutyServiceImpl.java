package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.MedicalTeamDuty;
import cn.lc.sunnyside.Service.MedicalTeamDutyService;
import cn.lc.sunnyside.mapper.MedicalTeamDutyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalTeamDutyServiceImpl implements MedicalTeamDutyService {

    private final MedicalTeamDutyMapper medicalTeamDutyMapper;

    @Override
    public List<MedicalTeamDuty> getOnDutyStaff(Long deptId) {
        return medicalTeamDutyMapper.selectByDeptIdAndDate(deptId, LocalDate.now());
    }
}
