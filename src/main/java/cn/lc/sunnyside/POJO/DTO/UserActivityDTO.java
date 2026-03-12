package cn.lc.sunnyside.POJO.DTO;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class UserActivityDTO {
    private Long activityId;
    private String activityName;
    private String location;
    private LocalTime startTime;
    private LocalTime endTime;
    private String participationStatus;
    private LocalDate activityDate;
}
