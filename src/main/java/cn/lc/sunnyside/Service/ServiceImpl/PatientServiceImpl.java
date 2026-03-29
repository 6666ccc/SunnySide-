package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.Patient;
import cn.lc.sunnyside.Service.PatientService;
import cn.lc.sunnyside.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientMapper patientMapper;

    @Override
    public Patient getById(Long id) {
        return patientMapper.selectById(id);
    }

    @Override
    public Patient getByAdmissionNo(String admissionNo) {
        return patientMapper.selectByAdmissionNo(admissionNo);
    }

    @Override
    public List<Patient> findByRef(String ref) {
        if (ref == null || ref.isBlank()) {
            return List.of();
        }
        return patientMapper.selectByRef(ref.trim());
    }
}
