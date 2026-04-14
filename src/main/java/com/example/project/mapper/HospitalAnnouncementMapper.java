package com.example.project.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.project.pojo.entity.HospitalAnnouncement;

@Mapper
public interface HospitalAnnouncementMapper {

    int insert(HospitalAnnouncement row);

    int updateById(HospitalAnnouncement row);

    int deleteById(@Param("id") Long id);

    HospitalAnnouncement selectById(@Param("id") Long id);

    List<HospitalAnnouncement> selectAll();

    /**
     * 全院公告：dept_id IS NULL；科室公告：dept_id 匹配；publishDateSince 非空时按发布日期下限过滤。
     */
    List<HospitalAnnouncement> selectAnnouncements(@Param("departmentId") Long departmentId,
            @Param("publishDateSince") LocalDate publishDateSince);
}
