package cn.lc.sunnyside.POJO.DO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class HealthRecord {
    private Long id;
    private Long elderId;
    private LocalDate recordDate;
    private LocalTime recordTime;
    private Integer systolicBp;
    private Integer diastolicBp;
    private Integer heartRate;
    private BigDecimal bloodSugar;
    private BigDecimal temperature;
    private String notes;
    private LocalDateTime createdAt;
}
