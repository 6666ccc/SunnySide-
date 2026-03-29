package cn.lc.sunnyside.Service.ServiceImpl;

import cn.lc.sunnyside.POJO.DO.Patient;
import cn.lc.sunnyside.POJO.DO.RelativePatientRelation;
import cn.lc.sunnyside.POJO.DO.RelativeUser;
import cn.lc.sunnyside.Service.RelativeAccessService;
import cn.lc.sunnyside.mapper.PatientMapper;
import cn.lc.sunnyside.mapper.RelativePatientRelationMapper;
import cn.lc.sunnyside.mapper.RelativeUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelativeAccessServiceImpl implements RelativeAccessService {

    private final RelativeUserMapper relativeUserMapper;
    private final RelativePatientRelationMapper relativePatientRelationMapper;
    private final PatientMapper patientMapper;

    @Override
    public boolean canAccessPatient(String relativePhone, Long patientId) {
        RelativeUser user = relativeUserMapper.selectByPhone(normalizePhone(relativePhone));
        if (user == null || patientId == null) {
            return false;
        }
        Integer count = relativePatientRelationMapper.existsRelation(user.getId(), patientId);
        return count != null && count > 0;
    }

    @Override
    public Long resolveDefaultPatientId(String relativePhone) {
        RelativeUser user = relativeUserMapper.selectByPhone(normalizePhone(relativePhone));
        if (user == null) {
            return null;
        }
        List<RelativePatientRelation> relations = relativePatientRelationMapper.selectByRelativeId(user.getId());
        if (relations == null || relations.isEmpty()) {
            return null;
        }
        List<RelativePatientRelation> proxyRelations = relations.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsLegalProxy()))
                .toList();
        if (proxyRelations.size() == 1) {
            return proxyRelations.get(0).getPatientId();
        }
        if (relations.size() == 1) {
            return relations.get(0).getPatientId();
        }
        return null;
    }

    @Override
    public Long resolvePatientIdByName(String relativePhone, String patientName) {
        RelativeUser user = relativeUserMapper.selectByPhone(normalizePhone(relativePhone));
        if (user == null) {
            return null;
        }
        List<RelativePatientRelation> relations = relativePatientRelationMapper.selectByRelativeId(user.getId());
        if (relations == null || relations.isEmpty()) {
            return null;
        }
        for (RelativePatientRelation rel : relations) {
            Patient patient = patientMapper.selectById(rel.getPatientId());
            if (patient != null && patientName.equals(patient.getPatientName())) {
                return patient.getId();
            }
        }
        for (RelativePatientRelation rel : relations) {
            Patient patient = patientMapper.selectById(rel.getPatientId());
            if (patient != null && patient.getPatientName() != null && patient.getPatientName().contains(patientName)) {
                return patient.getId();
            }
        }
        return null;
    }

    @Override
    public String buildBoundPatientContext(String relativePhone) {
        RelativeUser user = relativeUserMapper.selectByPhone(normalizePhone(relativePhone));
        if (user == null) {
            return "未识别到有效亲属账号。";
        }
        List<RelativePatientRelation> relations = relativePatientRelationMapper.selectByRelativeId(user.getId());
        if (relations == null || relations.isEmpty()) {
            return "当前亲属暂无已关联患者。";
        }

        LinkedHashMap<Long, RelativePatientRelation> relationByPatient = relations.stream()
                .collect(Collectors.toMap(RelativePatientRelation::getPatientId, r -> r, (a, b) -> a,
                        LinkedHashMap::new));

        List<String> summaries = relationByPatient.values().stream()
                .sorted(Comparator.comparing(RelativePatientRelation::getIsLegalProxy,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RelativePatientRelation::getPatientId))
                .map(rel -> {
                    Patient patient = patientMapper.selectById(rel.getPatientId());
                    if (patient == null) {
                        return "ID:" + rel.getPatientId();
                    }
                    String name = patient.getPatientName() != null ? patient.getPatientName() : "未知";
                    String suffix = Boolean.TRUE.equals(rel.getIsLegalProxy()) ? "(法定代理人)" : "";
                    return "ID:" + patient.getId() + " 姓名:" + name + " 床号:" + patient.getBedNumber() + suffix;
                })
                .toList();

        Long defaultPatientId = resolveDefaultPatientId(relativePhone);
        if (defaultPatientId != null) {
            return "已关联患者:" + String.join("；", summaries) + "。默认患者ID=" + defaultPatientId;
        }
        return "已关联患者:" + String.join("；", summaries) + "。存在多位关联患者，请让用户指明姓名。";
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }
}
