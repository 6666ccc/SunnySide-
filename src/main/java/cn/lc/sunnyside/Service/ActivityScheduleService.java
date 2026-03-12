package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.ActivitySchedule;
import java.util.List;

public interface ActivityScheduleService {
    List<ActivitySchedule> getTodayActivities();
}
