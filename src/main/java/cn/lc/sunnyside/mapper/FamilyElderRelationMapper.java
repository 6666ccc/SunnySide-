package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.FamilyElderRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FamilyElderRelationMapper {

    List<FamilyElderRelation> selectByFamilyId(@Param("familyId") Long familyId);

    List<FamilyElderRelation> selectByElderId(@Param("elderId") Long elderId);

    Integer existsRelation(@Param("familyId") Long familyId, @Param("elderId") Long elderId);
}
