package cn.lc.sunnyside.mapper;

import cn.lc.sunnyside.POJO.DO.FamilyElderRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FamilyElderRelationMapper {

    /**
     * 查询某家属绑定的全部老人关系。
     *
     * @param familyId 家属ID
     * @return 绑定关系列表
     */
    List<FamilyElderRelation> selectByFamilyId(@Param("familyId") Long familyId);

    /**
     * 查询某老人关联的全部家属关系。
     *
     * @param elderId 老人ID
     * @return 绑定关系列表
     */
    List<FamilyElderRelation> selectByElderId(@Param("elderId") Long elderId);

    /**
     * 判断家属与老人是否存在绑定关系。
     *
     * @param familyId 家属ID
     * @param elderId 老人ID
     * @return 关系记录数量
     */
    Integer existsRelation(@Param("familyId") Long familyId, @Param("elderId") Long elderId);
}
