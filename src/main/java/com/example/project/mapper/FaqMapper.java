package com.example.project.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.project.pojo.entity.Faq;

@Mapper
public interface FaqMapper {

    /**
     * category 为空时返回全部，按分类与排序权重排序。
     */
    List<Faq> selectByCategoryOptional(@Param("category") String category);
}
