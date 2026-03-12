package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.ActivityParticipation;
import cn.lc.sunnyside.POJO.DO.ActivitySchedule;
import cn.lc.sunnyside.POJO.DTO.UserActivityDTO;
import cn.lc.sunnyside.Service.ActivityParticipationService;
import cn.lc.sunnyside.mapper.ActivityParticipationMapper;
import cn.lc.sunnyside.mapper.ActivityScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityParticipationServiceImpl implements ActivityParticipationService {

    private final ActivityParticipationMapper activityParticipationMapper;
    private final ActivityScheduleMapper activityScheduleMapper;

    @Override
    public String registerActivity(Long elderId, Long activityId) {
        // Check if already registered
        LocalDate today = LocalDate.now();
        List<ActivityParticipation> existing = activityParticipationMapper.selectByElderIdAndDate(elderId, today);
        for (ActivityParticipation p : existing) {
            if (p.getActivityId().equals(activityId)) {
                return "Already registered for this activity.";
            }
        }

        // Get activity details for time
        ActivitySchedule activity = activityScheduleMapper.selectById(activityId);
        if (activity == null) {
            return "Activity not found.";
        }

        ActivityParticipation participation = new ActivityParticipation();
        participation.setElderId(elderId);
        participation.setActivityId(activityId);
        participation.setParticipationStatus("REGISTERED");
        participation.setCreatedAt(LocalDateTime.now());
        participation.setActivityDateStart(activity.getStartTime());
        participation.setActivityDateEnd(activity.getEndTime());
        participation.setActivityDate(today);

        try {
            activityParticipationMapper.insert(participation);
            return "Success";
        } catch (DuplicateKeyException e) {
            return "Already registered for this activity.";
        } catch (Exception e) {
            return "Failed to register: " + e.getMessage();
        }
    }

    @Override
    public String cancelActivityRegistration(Long elderId, Long activityId) {
        int updated = activityParticipationMapper.cancelRegistration(elderId, activityId);
        if (updated > 0) {
            return "Success";
        }
        return "Registration not found or cannot be canceled.";
    }

    @Override
    public List<UserActivityDTO> getMyActivityRegistrations(Long elderId, LocalDate date) {
        return activityParticipationMapper.selectUserActivitiesByElderIdAndDate(elderId, date);
    }
}
