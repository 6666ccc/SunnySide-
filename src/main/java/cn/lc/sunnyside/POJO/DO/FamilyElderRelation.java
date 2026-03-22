package cn.lc.sunnyside.POJO.DO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FamilyElderRelation {
    private Long id;
    private Long familyId;
    private Long elderId;
    private String relationType;
    private Boolean isPrimaryContact;
    private LocalDateTime createdAt;
}
