package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnnouncementMapper {

    /**
     * 查询最近公告。
     *
     * @param limit 返回条数
     * @return 公告列表
     */
    List<Announcement> selectRecent(@Param("limit") int limit);

    /**
     * 按条件查询公告。
     *
     * @param limit 返回条数
     * @param priority 优先级
     * @param activeOnly 是否仅返回有效公告
     * @return 公告列表
     */
    List<Announcement> selectByConditions(@Param("limit") int limit,
                                          @Param("priority") String priority,
                                          @Param("activeOnly") Boolean activeOnly);
}
