package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MedicalTeamDuty {
    private Long id;
    private Long deptId;
    private LocalDate dutyDate;
    private String staffName;
    private String staffRole;
    private String dutyTime;
    private String phone;
    private LocalDateTime createdAt;
}
