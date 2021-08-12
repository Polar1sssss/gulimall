package com.hujtb.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.hujtb.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.hujtb.gulimall.product.entity.AttrEntity;
import com.hujtb.gulimall.product.service.AttrAttrgroupRelationService;
import com.hujtb.gulimall.product.service.AttrService;
import com.hujtb.gulimall.product.service.CategoryService;
import com.hujtb.gulimall.product.vo.AttrGroupRelationVo;
import com.hujtb.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.hujtb.gulimall.product.entity.AttrGroupEntity;
import com.hujtb.gulimall.product.service.AttrGroupService;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.R;



/**
 * 属性分组
 *
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 17:55:32
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    AttrService attrService;

    @Autowired
    AttrAttrgroupRelationService relationService;

    /**
     * 列表
     */
    @RequestMapping("/list/{catalogId}")
    //@RequiresPermissions("product:attrgroup:list")
    public R list(@RequestParam Map<String, Object> params, @PathVariable Long catalogId){
        // PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params, catalogId);
        return R.ok().put("page", page);
    }

    @GetMapping("/{attrgroupId}/noattr/relation")
    public R attrRelation(@PathVariable("attrGroupId") Long attrGroupId,
                          @RequestParam Map<String, Object> params) {
        PageUtils page = attrService.getNoRelationAttr(attrGroupId, params);
        return R.ok().put("page", page);
    }

    @GetMapping("/{attrGroupId}/attr/relation")
    public R attrNoRelation(@PathVariable("attrGroupId") Long attrGroupId) {
        List<AttrEntity> list = attrService.getRelationAttr(attrGroupId);
        return R.ok().put("data", list);
    }

    @GetMapping("/{catalogId}/withattr")
    public R getAttrGroupWithAttrs(@PathVariable("catalogId") Long catalogId) {
        List<AttrGroupWithAttrsVo> voList = attrGroupService.getAttrGroupWithAttrsByCategoryId(catalogId);
        return R.ok().put("data", voList);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    //@RequiresPermissions("product:attrgroup:info")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

        Long catalogId = attrGroup.getCatalogId();
        Long[] catalogPath = categoryService.findPathById(catalogId);
        attrGroup.setcatalogPath(catalogPath);
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attrgroup:save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);
        return R.ok();
    }

    /**
     * 新增关联
     */
    @RequestMapping("/attr/relation")
    //@RequiresPermissions("product:attrgroup:save")
    public R addRelation(@RequestBody AttrGroupRelationVo[] vos){
        relationService.addRelation(vos);
        return R.ok();
    }


    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attrgroup:update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attrgroup:delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

    @RequestMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody AttrGroupRelationVo[] vos) {
        attrGroupService.deleteRelation(vos);
        return R.ok();
    }

}
