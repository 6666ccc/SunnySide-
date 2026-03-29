package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Patient {
    private Long id;
    private Long deptId;
    private String patientName;
    private String gender;
    private String admissionNo;
    private String bedNumber;
    private LocalDate admissionDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
