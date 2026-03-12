package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.VisitAppointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VisitAppointmentMapper {

    int insert(VisitAppointment visitAppointment);

    List<VisitAppointment> selectByElderId(@Param("elderId") Long elderId);

    int cancelByIdAndElderId(@Param("appointmentId") Long appointmentId, @Param("elderId") Long elderId);

    List<VisitAppointment> selectByConditions(@Param("elderId") Long elderId,
                                              @Param("status") String status,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);
}
