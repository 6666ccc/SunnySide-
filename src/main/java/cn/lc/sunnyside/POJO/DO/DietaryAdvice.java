package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DietaryAdvice {
    private Long id;
    private Long patientId;
    private LocalDate mealDate;
    private String mealType;
    private String foodContent;
    private String nutritionNotes;
    private LocalDateTime createdAt;
}
