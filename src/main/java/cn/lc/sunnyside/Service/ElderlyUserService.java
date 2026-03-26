package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.ElderlyUser;
import java.util.List;

/**
 * 老人基础信息查询服务抽象。
 */
public interface ElderlyUserService {
    /**
     * 查询老人当前位置。
     *
     * @param elderId 老人ID
     * @return 位置描述
     */
    String getElderLocation(Long elderId);

    /**
     * 查询老人健康提醒信息。
     *
     * @param elderId 老人ID
     * @return 提醒文案
     */
    String getHealthReminder(Long elderId);

    /**
     * 按ID查询老人详情。
     *
     * @param id 老人ID
     * @return 老人实体
     */
    ElderlyUser getById(Long id);

    /**
     * 按姓氏查询老人。
     *
     * @param surname 姓氏
     * @return 老人实体
     */
    ElderlyUser findElderlyUserBySurname(String surname);

    /**
     * 按身份线索查询候选老人列表。
     *
     * @param ref 身份线索
     * @return 候选老人列表
     */
    List<ElderlyUser> findByRef(String ref);
}
