package cn.lc.sunnyside.Service;

import java.time.LocalDate;

public interface FamilyAccessService {

    boolean canAccessElder(String familyPhone, Long elderId);

    String getElderDailySummary(String familyPhone, Long elderId, LocalDate date);

    Long resolveDefaultElderId(String familyPhone);

    String buildBoundElderContext(String familyPhone);
}
