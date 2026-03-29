package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class HospitalAnnouncement {
    private Long id;
    private Long deptId;
    private String title;
    private String content;
    private LocalDate publishDate;
    private String priority;
    private LocalDateTime createdAt;
}
