package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.ActivitySchedule;
import java.util.List;

/**
 * 活动排期服务抽象。
 */
public interface ActivityScheduleService {
    /**
     * 查询今日活动安排。
     *
     * @return 活动列表
     */
    List<ActivitySchedule> getTodayActivities();
}
