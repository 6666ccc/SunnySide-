package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.HospitalAnnouncement;
import cn.lc.sunnyside.Service.HospitalAnnouncementService;
import cn.lc.sunnyside.mapper.HospitalAnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HospitalAnnouncementServiceImpl implements HospitalAnnouncementService {

    private final HospitalAnnouncementMapper hospitalAnnouncementMapper;

    @Override
    public List<HospitalAnnouncement> getRecentAnnouncements(int limit) {
        return hospitalAnnouncementMapper.selectRecent(limit);
    }

    @Override
    public List<HospitalAnnouncement> getByConditions(int limit, String priority, Long deptId) {
        return hospitalAnnouncementMapper.selectByConditions(limit, priority, deptId);
    }

    @Override
    public List<HospitalAnnouncement> getGlobalAndDeptAnnouncements(Long deptId, int limit) {
        return hospitalAnnouncementMapper.selectGlobalAndDept(deptId, limit);
    }
}
