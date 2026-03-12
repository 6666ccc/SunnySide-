package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.Announcement;
import cn.lc.sunnyside.Service.AnnouncementService;
import cn.lc.sunnyside.mapper.AnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    @Override
    public List<Announcement> getRecentAnnouncements(int limit) {
        return announcementMapper.selectRecent(limit);
    }

    @Override
    public List<Announcement> getAnnouncements(int limit, String priority, Boolean activeOnly) {
        return announcementMapper.selectByConditions(limit, priority, activeOnly);
    }
}
