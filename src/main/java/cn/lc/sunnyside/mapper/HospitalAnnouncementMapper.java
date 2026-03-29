package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.HospitalAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HospitalAnnouncementMapper {

    List<HospitalAnnouncement> selectRecent(@Param("limit") int limit);

    List<HospitalAnnouncement> selectByDeptId(@Param("deptId") Long deptId, @Param("limit") int limit);

    /**
     * 查询全院公告（dept_id = 0）及指定科室的公告。
     */
    List<HospitalAnnouncement> selectGlobalAndDept(@Param("deptId") Long deptId, @Param("limit") int limit);

    List<HospitalAnnouncement> selectByConditions(@Param("limit") int limit,
                                                  @Param("priority") String priority,
                                                  @Param("deptId") Long deptId);
}
