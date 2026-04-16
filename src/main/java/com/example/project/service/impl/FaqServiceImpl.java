package com.example.project.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.project.mapper.FaqMapper;
import com.example.project.pojo.entity.Faq;
import com.example.project.service.FaqService;

@Service
public class FaqServiceImpl implements FaqService {

    @Autowired
    private FaqMapper faqMapper;

    @Override
    public List<Faq> listByCategoryOptional(String category) {
        return faqMapper.selectByCategoryOptional(category);
    }
}
