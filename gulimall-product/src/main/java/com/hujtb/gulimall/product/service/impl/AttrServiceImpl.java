package com.hujtb.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hujtb.common.constant.ProductConst;
import com.hujtb.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.hujtb.gulimall.product.dao.AttrGroupDao;
import com.hujtb.gulimall.product.dao.CategoryDao;
import com.hujtb.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.hujtb.gulimall.product.entity.AttrGroupEntity;
import com.hujtb.gulimall.product.entity.CategoryEntity;
import com.hujtb.gulimall.product.service.CategoryService;
import com.hujtb.gulimall.product.vo.AttrResponseVo;
import com.hujtb.gulimall.product.vo.AttrVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.Query;

import com.hujtb.gulimall.product.dao.AttrDao;
import com.hujtb.gulimall.product.entity.AttrEntity;
import com.hujtb.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationDao relationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;

    final int ATTR_BASE = ProductConst.AttrConst.ATTR_BASE.getCode();
    final int ATTR_SALE = ProductConst.AttrConst.ATTR_SALE.getCode();

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        // ??????????????????
        this.save(attrEntity);
        // ???????????????????????????????????????id?????????????????????????????????
        if (attr.getAttrType() == ATTR_BASE && attr.getAttrGroupId() != null) {
            // ??????????????????
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            // ??????????????????
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            // ???AttrEntity????????????????????????
            relationEntity.setAttrId(attrEntity.getAttrId());
            relationDao.insert(relationEntity);
        }
    }

    @Override
    public PageUtils queryBaseAttrList(Map<String, Object> params, Long catalogId, String attrType) {
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>()
                .eq("attr_type", "base".equalsIgnoreCase(attrType) ? ATTR_BASE : ATTR_SALE);
        if (catalogId != 0) {
            queryWrapper.eq("catalog_id", catalogId);
        }
        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)) {
            queryWrapper.and((wrapper) -> {
                wrapper.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), queryWrapper);
        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttrResponseVo> voList = records.stream().map((attrEntity) -> {
            AttrResponseVo attrResponseVo = new AttrResponseVo();
            BeanUtils.copyProperties(attrEntity, attrResponseVo);

            if ("base".equalsIgnoreCase(attrType)) {
                AttrAttrgroupRelationEntity attrId = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
                if (attrId != null && attrId.getAttrGroupId() != null) {
                    AttrGroupEntity groupEntity = attrGroupDao.selectById(attrId.getAttrGroupId());
                    attrResponseVo.setAttrGroupName(groupEntity.getAttrGroupName());
                }
            }

            // ??????categoryId
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatalogId());
            if (categoryEntity != null) {
                attrResponseVo.setCatalogName(categoryEntity.getName());
            }
            return attrResponseVo;
        }).collect(Collectors.toList());

        pageUtils.setList(voList);
        return pageUtils;
    }

    /**
     * ????????????id???????????? + ???????????? + ????????????id
     *
     * @param attrId
     * @return
     */
    @Override
    public AttrResponseVo getAttrInfo(Long attrId) {
        AttrResponseVo attrResponseVo = new AttrResponseVo();
        AttrEntity attrEntity = this.getById(attrId);

        BeanUtils.copyProperties(attrEntity, attrResponseVo);

        if (attrEntity.getAttrType() == ATTR_BASE) {
            // ????????????????????????
            AttrAttrgroupRelationEntity relationEntity = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if (relationEntity != null) {
                Long attrGroupId = relationEntity.getAttrGroupId();
                attrResponseVo.setAttrGroupId(attrGroupId);
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrGroupId);
                if (attrGroupEntity != null) {
                    attrResponseVo.setAttrGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }
        // ??????????????????
        Long catalogId = attrEntity.getCatalogId();
        Long[] pathById = categoryService.findPathById(catalogId);
        attrResponseVo.setCatalogPath(pathById);

        CategoryEntity categoryEntity = categoryDao.selectById(catalogId);
        if (categoryEntity != null) {
            attrResponseVo.setCatalogName(categoryEntity.getName());
        }
        return attrResponseVo;
    }

    /**
     * ????????????
     *
     * @param attr
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateAttr(AttrVo attr) {

        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.updateById(attrEntity);

        if (attrEntity.getAttrType() == ATTR_BASE) {
            //1?????????????????????
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attr.getAttrId());

            Integer count = relationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>()
                    .eq("attr_id", attr.getAttrId()));

            if (count > 0) {
                relationDao.update(relationEntity,
                        new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            } else {
                relationDao.insert(relationEntity);
            }
        }
    }

    /**
     * ??????????????????id???????????????????????????
     *
     * @param attrGroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrGroupId) {
        List<AttrAttrgroupRelationEntity> attrgroupRelationEntity = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrGroupId));
        List<Long> attrIds = attrgroupRelationEntity.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());
        if (attrIds == null || attrIds.size() == 0) {
            return null;
        }
        List<AttrEntity> attrEntities = this.listByIds(attrIds);
        return attrEntities;
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param attrGroupId
     * @param params
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Long attrGroupId, Map<String, Object> params) {
        // 1.?????????????????????????????????????????????????????????
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrGroupId);
        Long catalogId = attrGroupEntity.getCatalogId();
        // 2.?????????????????????????????????????????????????????????
        // 1)??????????????????????????????
        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>()
                .eq("catalog_id", catalogId));
        List<Long> groupIds = attrGroupEntities.stream().map((item) -> {
            return item.getAttrGroupId();
        }).collect(Collectors.toList());
        // 2)???????????????????????????
        List<AttrAttrgroupRelationEntity> relationEntities = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>()
                .in("attr_group_id", groupIds));
        List<Long> attrIds = relationEntities.stream().map((item) -> {
            return item.getAttrId();
        }).collect(Collectors.toList());
        // 3)???????????????????????????????????????????????????
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>()
                .eq("catalog_id", catalogId)
                .eq("attr_type", ATTR_BASE);
        if (attrIds != null && attrIds.size() > 0) {
            queryWrapper.notIn("attr_id", attrIds);
        }
        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)) {
            queryWrapper.and((w) -> {
                w.eq("attr_id", key).like("attr_name", key);
            });
        }
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), queryWrapper);
        PageUtils pageUtils = new PageUtils(page);
        return pageUtils;
    }

    @Override
    public List<Long> selectSearchAttrIds(List<Long> attrIds) {
        return baseMapper.selectSearchAttrIds(attrIds);
    }

}