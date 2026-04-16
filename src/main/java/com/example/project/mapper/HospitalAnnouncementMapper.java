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
     * 按 publish_date 下限查询；departmentId 非空时含全院(dept_id IS NULL)与该院系科室；为空时不限科室。
     * publishDateSince 由调用方保证非空（如近 30 日窗口下限）。
     */
    List<HospitalAnnouncement> selectAnnouncements(@Param("departmentId") Long departmentId,
            @Param("publishDateSince") LocalDate publishDateSince);
}
