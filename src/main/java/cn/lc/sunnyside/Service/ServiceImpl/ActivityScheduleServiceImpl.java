package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.ActivitySchedule;
import cn.lc.sunnyside.Service.ActivityScheduleService;
import cn.lc.sunnyside.mapper.ActivityScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityScheduleServiceImpl implements ActivityScheduleService {

    private final ActivityScheduleMapper activityScheduleMapper;

    @Override
    public List<ActivitySchedule> getTodayActivities() {
        return activityScheduleMapper.selectByDate(LocalDate.now());
    }
}
