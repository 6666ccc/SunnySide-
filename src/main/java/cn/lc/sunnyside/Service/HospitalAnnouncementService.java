package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.HospitalAnnouncement;
import java.util.List;

public interface HospitalAnnouncementService {

    List<HospitalAnnouncement> getRecentAnnouncements(int limit);

    List<HospitalAnnouncement> getByConditions(int limit, String priority, Long deptId);

    List<HospitalAnnouncement> getGlobalAndDeptAnnouncements(Long deptId, int limit);
}
