package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class TreatmentPlan {
    private Long id;
    private Long patientId;
    private String taskName;
    private String description;
    private LocalDate planDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private String category;
    private Boolean isCompleted;
    private LocalDateTime createdAt;
}
