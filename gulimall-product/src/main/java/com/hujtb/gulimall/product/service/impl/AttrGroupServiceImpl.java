package com.hujtb.gulimall.product.service.impl;

import com.fasterxml.jackson.databind.util.BeanUtil;
import com.hujtb.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.hujtb.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.hujtb.gulimall.product.entity.AttrEntity;
import com.hujtb.gulimall.product.service.AttrService;
import com.hujtb.gulimall.product.vo.AttrGroupRelationVo;
import com.hujtb.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.hujtb.gulimall.product.vo.SkuItemVo;
import com.hujtb.gulimall.product.vo.SpuItemAttrGroupVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.Query;

import com.hujtb.gulimall.product.dao.AttrGroupDao;
import com.hujtb.gulimall.product.entity.AttrGroupEntity;
import com.hujtb.gulimall.product.service.AttrGroupService;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrAttrgroupRelationDao relationDao;

    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 根据选择的目录查询属性分组信息
     *
     * @param params
     * @param catalogId
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catalogId) {
        IPage<AttrGroupEntity> page = null;
        String key = (String) params.get("key");
        // select * from pms_attr_group where catalog_id = xxx and (attr_group_id = key or attr_group_name like %key%);
        QueryWrapper<AttrGroupEntity> queryWrapper = new QueryWrapper<AttrGroupEntity>();
        // 如果搜索框有值则拼接一下条件
        if (!("").equals(key)) {
            queryWrapper.and((obj) -> {
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        }
        if (catalogId == 0) {
            page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    queryWrapper
            );
        } else {
            queryWrapper.eq("catalog_id", catalogId);
            page = this.page(new Query<AttrGroupEntity>().getPage(params), queryWrapper);
        }
        return new PageUtils(page);
    }

    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {
        List<AttrAttrgroupRelationEntity> entities = Arrays.asList(vos).stream().map((vo) -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(vo, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());
        relationDao.deleteBatchRelation(entities);
    }

    /**
     * 根据分类id查出所有分组及这些组里的属性
     *
     * @param catalogId
     * @return
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCategoryId(Long catalogId) {
        // 查出当前分类下所有属性分组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catalog_id", catalogId));
        // 查出每个属性分组对应的属性
        List<AttrGroupWithAttrsVo> attrGroupWithAttrsVos = attrGroupEntities.stream().map((item) -> {
            AttrGroupWithAttrsVo attrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(item, attrsVo);
            List<AttrEntity> attrs = attrService.getRelationAttr(attrsVo.getAttrGroupId());
            attrsVo.setAttrs(attrs);
            return attrsVo;
        }).collect(Collectors.toList());
        return attrGroupWithAttrsVos;
    }

    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        AttrGroupDao baseMapper = this.getBaseMapper();
        List<SpuItemAttrGroupVo> groupVos = baseMapper.getAttrGroupWithAttrsBySpuId(spuId, catalogId);
        return groupVos;
    }

}