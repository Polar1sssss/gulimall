package com.hujtb.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.gulimall.product.entity.AttrGroupEntity;
import com.hujtb.gulimall.product.vo.AttrGroupRelationVo;
import com.hujtb.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.hujtb.gulimall.product.vo.SkuItemVo;
import com.hujtb.gulimall.product.vo.SpuItemAttrGroupVo;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 17:55:32
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long catalogId);

    void deleteRelation(AttrGroupRelationVo[] vos);

    List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCategoryId(Long catalogId);

    List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId);
}

