package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.VisitAppointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VisitAppointmentMapper {

    /**
     * 新增探访预约记录。
     *
     * @param visitAppointment 预约实体
     * @return 受影响行数
     */
    int insert(VisitAppointment visitAppointment);

    /**
     * 查询指定老人的全部探访预约。
     *
     * @param elderId 老人ID
     * @return 预约列表
     */
    List<VisitAppointment> selectByElderId(@Param("elderId") Long elderId);

    /**
     * 按预约ID与老人ID取消预约。
     *
     * @param appointmentId 预约ID
     * @param elderId 老人ID
     * @return 受影响行数
     */
    int cancelByIdAndElderId(@Param("appointmentId") Long appointmentId, @Param("elderId") Long elderId);

    /**
     * 按条件查询探访预约。
     *
     * @param elderId 老人ID
     * @param status 预约状态
     * @param from 起始时间
     * @param to 结束时间
     * @return 预约列表
     */
    List<VisitAppointment> selectByConditions(@Param("elderId") Long elderId,
                                              @Param("status") String status,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);
}
