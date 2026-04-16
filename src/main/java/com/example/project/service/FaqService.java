package com.example.project.service;

import java.util.List;

import com.example.project.pojo.entity.Faq;

public interface FaqService {

    /**
     * @param category 为 null 或空白时返回全部 FAQ
     */
    List<Faq> listByCategoryOptional(String category);
}
