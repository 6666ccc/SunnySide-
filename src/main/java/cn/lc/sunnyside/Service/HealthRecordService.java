package cn.lc.sunnyside.Service;

import java.time.LocalDate;

/**
 * 健康记录服务抽象。
 */
public interface HealthRecordService {

    /**
     * 在家属权限范围内查询老人健康记录并返回可读摘要。
     *
     * @param familyPhone 家属手机号
     * @param elderId 老人ID
     * @param startDate 起始日期
     * @param endDate 结束日期
     * @return 健康数据摘要
     */
    String queryElderHealth(String familyPhone, Long elderId, LocalDate startDate, LocalDate endDate);
}
