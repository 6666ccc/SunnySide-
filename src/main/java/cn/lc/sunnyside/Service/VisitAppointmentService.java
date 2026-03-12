package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.VisitAppointment;
import java.time.LocalDateTime;
import java.util.List;

public interface VisitAppointmentService {
    String bookVisit(Long elderId, String visitorName, String phone, LocalDateTime time, String relation);
    List<VisitAppointment> getVisitors(Long elderId);
    String cancelVisitAppointment(Long elderId, Long appointmentId);
    List<VisitAppointment> queryVisitAppointments(Long elderId, String status, LocalDateTime from, LocalDateTime to);
}
