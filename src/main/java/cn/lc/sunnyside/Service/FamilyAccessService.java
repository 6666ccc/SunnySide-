package cn.lc.sunnyside.Service;

import java.time.LocalDate;

/**
 * 家属访问控制与家属视角聚合查询服务抽象。
 */
public interface FamilyAccessService {

    /**
     * 判断家属是否可访问指定老人。
     *
     * @param familyPhone 家属手机号
     * @param elderId 老人ID
     * @return 是否有访问权限
     */
    boolean canAccessElder(String familyPhone, Long elderId);

    /**
     * 查询老人某日概览信息。
     *
     * @param familyPhone 家属手机号
     * @param elderId 老人ID
     * @param date 目标日期
     * @return 面向家属展示的摘要文本
     */
    String getElderDailySummary(String familyPhone, Long elderId, LocalDate date);

    /**
     * 解析家属默认绑定老人ID。
     *
     * @param familyPhone 家属手机号
     * @return 默认老人ID
     */
    Long resolveDefaultElderId(String familyPhone);

    /**
     * 在家属绑定范围内按姓名解析老人ID。
     *
     * @param familyPhone 家属手机号
     * @param elderName 老人姓名
     * @return 老人ID，未命中返回 null
     */
    Long resolveElderIdByName(String familyPhone, String elderName);

    /**
     * 构建家属已绑定老人的上下文文本。
     *
     * @param familyPhone 家属手机号
     * @return 绑定关系描述文本
     */
    String buildBoundElderContext(String familyPhone);
}
