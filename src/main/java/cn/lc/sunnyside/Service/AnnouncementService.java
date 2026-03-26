package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.Announcement;
import java.util.List;

/**
 * 公告查询服务抽象。
 */
public interface AnnouncementService {
    /**
     * 查询最近公告。
     *
     * @param limit 返回条数
     * @return 公告列表
     */
    List<Announcement> getRecentAnnouncements(int limit);

    /**
     * 按条件查询公告。
     *
     * @param limit 返回条数
     * @param priority 公告优先级
     * @param activeOnly 是否仅返回有效公告
     * @return 公告列表
     */
    List<Announcement> getAnnouncements(int limit, String priority, Boolean activeOnly);
}
