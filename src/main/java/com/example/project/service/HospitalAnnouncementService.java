package com.example.project.service;

import java.time.LocalDate;
import java.util.List;

import com.example.project.pojo.entity.HospitalAnnouncement;

public interface HospitalAnnouncementService {

    int save(HospitalAnnouncement row);

    int update(HospitalAnnouncement row);

    int removeById(Long id);

    HospitalAnnouncement getById(Long id);

    List<HospitalAnnouncement> listAll();

    /**
     * 公告列表。仅返回 {@code publish_date} 在「近 30 日」内的记录（含今日起向前共 30 个自然日）；
     * {@code publishDateSince} 若早于该窗口则抬升到窗口起点；若为空则使用窗口起点。
     * {@code departmentId} 非空：全院 + 该院系科室；为空：不限科室（全院及各科室）。
     */
    List<HospitalAnnouncement> listAnnouncements(Long departmentId, LocalDate publishDateSince);
}
