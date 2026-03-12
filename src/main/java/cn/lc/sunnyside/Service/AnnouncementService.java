package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.Announcement;
import java.util.List;

public interface AnnouncementService {
    List<Announcement> getRecentAnnouncements(int limit);
    List<Announcement> getAnnouncements(int limit, String priority, Boolean activeOnly);
}
