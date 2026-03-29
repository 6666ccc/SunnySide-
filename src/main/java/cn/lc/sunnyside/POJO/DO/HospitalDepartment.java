package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HospitalDepartment {
    private Long id;
    private String deptName;
    private String contactPhone;
    private String location;
    private LocalDateTime createdAt;
}
