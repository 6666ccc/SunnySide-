package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DTO.UserActivityDTO;

import java.time.LocalDate;
import java.util.List;

public interface ActivityParticipationService {
    String registerActivity(Long elderId, Long activityId);
    String cancelActivityRegistration(Long elderId, Long activityId);
    List<UserActivityDTO> getMyActivityRegistrations(Long elderId, LocalDate date);
}
