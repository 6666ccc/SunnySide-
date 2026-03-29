package cn.lc.sunnyside.POJO.DO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RelativePatientRelation {
    private Long id;
    private Long relativeId;
    private Long patientId;
    private String relationType;
    private Boolean isLegalProxy;
    private LocalDateTime createdAt;
}
