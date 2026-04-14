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

    List<HospitalAnnouncement> listAnnouncements(Long departmentId, LocalDate publishDateSince);
}
