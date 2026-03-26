package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.MedicalDuty;
import java.util.List;

/**
 * 医护值班查询服务抽象。
 */
public interface MedicalDutyService {
    /**
     * 查询当天值班医护人员。
     *
     * @return 值班列表
     */
    List<MedicalDuty> getOnDutyStaff();
}
