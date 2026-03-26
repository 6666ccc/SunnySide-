package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.VisitAppointment;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 探访预约服务抽象。
 */
public interface VisitAppointmentService {
    /**
     * 新增探访预约。
     *
     * @param elderId 老人ID
     * @param visitorName 访客姓名
     * @param phone 联系电话
     * @param time 来访时间
     * @param relation 与老人关系
     * @return 执行结果文案
     */
    String bookVisit(Long elderId, String visitorName, String phone, LocalDateTime time, String relation);

    /**
     * 查询老人近期来访预约。
     *
     * @param elderId 老人ID
     * @return 预约列表
     */
    List<VisitAppointment> getVisitors(Long elderId);

    /**
     * 取消指定探访预约。
     *
     * @param elderId 老人ID
     * @param appointmentId 预约ID
     * @return 执行结果文案
     */
    String cancelVisitAppointment(Long elderId, Long appointmentId);

    /**
     * 按条件查询探访预约。
     *
     * @param elderId 老人ID
     * @param status 预约状态
     * @param from 开始时间
     * @param to 结束时间
     * @return 预约列表
     */
    List<VisitAppointment> queryVisitAppointments(Long elderId, String status, LocalDateTime from, LocalDateTime to);
}
