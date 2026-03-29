package cn.lc.sunnyside.Service;

public interface RelativeAccessService {

    boolean canAccessPatient(String relativePhone, Long patientId);

    Long resolveDefaultPatientId(String relativePhone);

    Long resolvePatientIdByName(String relativePhone, String patientName);

    String buildBoundPatientContext(String relativePhone);
}
