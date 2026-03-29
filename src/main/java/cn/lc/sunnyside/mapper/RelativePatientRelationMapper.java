package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.RelativePatientRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RelativePatientRelationMapper {

    List<RelativePatientRelation> selectByRelativeId(@Param("relativeId") Long relativeId);

    List<RelativePatientRelation> selectByPatientId(@Param("patientId") Long patientId);

    Integer existsRelation(@Param("relativeId") Long relativeId, @Param("patientId") Long patientId);
}
