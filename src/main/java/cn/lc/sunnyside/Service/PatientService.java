package cn.lc.sunnyside.Service;

import cn.lc.sunnyside.POJO.DO.Patient;
import java.util.List;

public interface PatientService {

    Patient getById(Long id);

    Patient getByAdmissionNo(String admissionNo);

    List<Patient> findByRef(String ref);
}
