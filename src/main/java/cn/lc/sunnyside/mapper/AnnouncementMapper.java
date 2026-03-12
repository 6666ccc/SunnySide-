package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnnouncementMapper {

    List<Announcement> selectRecent(@Param("limit") int limit);

    List<Announcement> selectByConditions(@Param("limit") int limit,
                                          @Param("priority") String priority,
                                          @Param("activeOnly") Boolean activeOnly);
}
