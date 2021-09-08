package com.hujtb.gulimall.search.service;

import com.hujtb.gulimall.search.vo.SearchParamVo;
import com.hujtb.gulimall.search.vo.SearchResponseVo;

public interface MallSearchService {
    SearchResponseVo search(SearchParamVo searchParamVo);
}
