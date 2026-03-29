package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.MedicalTeamDuty;
import java.util.List;

public interface MedicalTeamDutyService {

    List<MedicalTeamDuty> getOnDutyStaff(Long deptId);
}
