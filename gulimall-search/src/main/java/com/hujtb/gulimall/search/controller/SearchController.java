package com.hujtb.gulimall.search.controller;

import com.hujtb.gulimall.search.service.MallSearchService;
import com.hujtb.gulimall.search.vo.SearchParamVo;
import com.hujtb.gulimall.search.vo.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    @GetMapping("/list.html")
    public String listPage(SearchParamVo searchParamVo, Model model) {
        SearchResponseVo result = mallSearchService.search(searchParamVo);
        model.addAttribute("result", result);
        return "list";
    }
}
