package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.Patient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PatientMapper {

    Patient selectById(@Param("id") Long id);

    Patient selectByAdmissionNo(@Param("admissionNo") String admissionNo);

    List<Patient> selectByDeptId(@Param("deptId") Long deptId);

    List<Patient> selectByRef(@Param("ref") String ref);
}
