package com.example.project.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.project.pojo.entity.Patient;
import com.example.project.pojo.vo.PatientBasicInfoVo;

@Mapper
public interface PatientMapper {

    int insert(Patient row);

    int updateById(Patient row);

    int deleteById(@Param("id") Long id);

    Patient selectById(@Param("id") Long id);

    List<Patient> selectAll();

    PatientBasicInfoVo selectBasicInfoWithDept(@Param("patientId") Long patientId);

    /**
     * 按住院号查询在院患者（仅 {@code IN_HOSPITAL}），用于家属绑定/搜索。
     */
    Patient selectByAdmissionNo(@Param("admissionNo") String admissionNo);

    /** 按住院号查询患者（不限状态），用于患者注册验证 */
    Patient selectByAdmissionNoForRegister(@Param("admissionNo") String admissionNo);

    String selectPasswordByUsername(@Param("username") String username);

    Long selectIdByUsername(@Param("username") String username);

    int updateUsernameAndPassword(@Param("id") Long id,
                                  @Param("username") String username,
                                  @Param("passwordHash") String passwordHash);
}
