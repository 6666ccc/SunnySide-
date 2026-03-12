package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.VisitAppointment;
import cn.lc.sunnyside.Service.VisitAppointmentService;
import cn.lc.sunnyside.mapper.VisitAppointmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisitAppointmentServiceImpl implements VisitAppointmentService {

    private final VisitAppointmentMapper visitAppointmentMapper;

    @Override
    public String bookVisit(Long elderId, String visitorName, String phone, LocalDateTime time, String relation) {
        VisitAppointment appointment = new VisitAppointment();
        appointment.setElderId(elderId);
        appointment.setVisitorName(visitorName);
        appointment.setVisitorPhone(phone);
        appointment.setVisitTime(time);
        appointment.setStatus("PENDING"); // Default status
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setRelation(relation);

        visitAppointmentMapper.insert(appointment);
        return "Success";
    }

    @Override
    public List<VisitAppointment> getVisitors(Long elderId) {
        List<VisitAppointment> allVisits = visitAppointmentMapper.selectByElderId(elderId);
        LocalDateTime now = LocalDateTime.now();
        return allVisits.stream()
                .filter(v -> v.getVisitTime().isAfter(now))
                .collect(Collectors.toList());
    }

    @Override
    public String cancelVisitAppointment(Long elderId, Long appointmentId) {
        int updated = visitAppointmentMapper.cancelByIdAndElderId(appointmentId, elderId);
        if (updated > 0) {
            return "Success";
        }
        return "Appointment not found or cannot be canceled.";
    }

    @Override
    public List<VisitAppointment> queryVisitAppointments(Long elderId, String status, LocalDateTime from, LocalDateTime to) {
        return visitAppointmentMapper.selectByConditions(elderId, status, from, to);
    }
}
