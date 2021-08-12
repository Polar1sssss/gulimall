package com.hujtb.gulimall.search.service;

import com.hujtb.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

public interface ProductSaveService {

    boolean productStatusUp(List<SkuEsModel> esModelList) throws IOException;
}
