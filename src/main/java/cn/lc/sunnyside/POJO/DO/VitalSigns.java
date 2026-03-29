package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class VitalSigns {
    private Long id;
    private Long patientId;
    private LocalDate recordDate;
    private LocalTime recordTime;
    private Integer systolicBp;
    private Integer diastolicBp;
    private Integer heartRate;
    private BigDecimal bloodSugar;
    private BigDecimal temperature;
    private Integer bloodOxygen;
    private String recordedBy;
    private String notes;
    private LocalDateTime createdAt;
}
