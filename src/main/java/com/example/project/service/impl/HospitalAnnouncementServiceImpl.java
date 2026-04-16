package com.example.project.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.project.mapper.HospitalAnnouncementMapper;
import com.example.project.pojo.entity.HospitalAnnouncement;
import com.example.project.service.HospitalAnnouncementService;

/* 医院公告服务实现类 */
@Service
public class HospitalAnnouncementServiceImpl implements HospitalAnnouncementService {

    @Autowired
    private HospitalAnnouncementMapper hospitalAnnouncementMapper;

    /* 保存医院公告 */
    @Override
    public int save(HospitalAnnouncement row) {
        return hospitalAnnouncementMapper.insert(row);
    }

    /* 更新医院公告 */
    @Override
    public int update(HospitalAnnouncement row) {
        return hospitalAnnouncementMapper.updateById(row);
    }

    /* 删除医院公告 */
    @Override
    public int removeById(Long id) {
        return hospitalAnnouncementMapper.deleteById(id);
    }

    /* 根据ID获取医院公告 */
    @Override
    public HospitalAnnouncement getById(Long id) {
        return hospitalAnnouncementMapper.selectById(id);
    }

    /* 获取所有医院公告 */
    @Override
    public List<HospitalAnnouncement> listAll() {
        return hospitalAnnouncementMapper.selectAll();
    }

    private static final int ANNOUNCEMENT_LOOKBACK_DAYS = 30;

    /* 根据部门ID与发布日期下限获取医院公告（仅近 30 日内 publish_date） */
    @Override
    public List<HospitalAnnouncement> listAnnouncements(Long departmentId, LocalDate publishDateSince) {
        LocalDate windowStart = LocalDate.now().minusDays(ANNOUNCEMENT_LOOKBACK_DAYS - 1L);
        LocalDate effectiveSince =
                publishDateSince == null ? windowStart : publishDateSince.isBefore(windowStart) ? windowStart : publishDateSince;
        return hospitalAnnouncementMapper.selectAnnouncements(departmentId, effectiveSince);
    }
}
