package com.hujtb.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.hujtb.gulimall.product.entity.ProductAttrValueEntity;
import com.hujtb.gulimall.product.service.ProductAttrValueService;
import com.hujtb.gulimall.product.vo.AttrResponseVo;
import com.hujtb.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.hujtb.gulimall.product.service.AttrService;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.R;



/**
 * 商品属性
 *
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 17:55:32
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {

    @Autowired
    private AttrService attrService;

    @Autowired
    ProductAttrValueService valueService;

    @GetMapping("/base/listforspu/{spuId}")
    public R baseAttrListforspu(@PathVariable("spuId") Long spuId) {
        List<ProductAttrValueEntity> entityList = valueService.baseAttrListforspu(spuId);
        return R.ok().put("data", entityList);
    }

    @GetMapping("/{attrType}/list/{catalogId}")
    public R baseAttrList(@RequestParam Map<String, Object> params,
                          @PathVariable("attrType") String attrType,
                          @PathVariable("catalogId") Long catalogId) {
        PageUtils page = attrService.queryBaseAttrList(params, catalogId, attrType);
        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId){
		AttrResponseVo attr = attrService.getAttrInfo(attrId);

        return R.ok().put("attr", attr);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrVo attr){
		attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrVo attr){
		// attrService.updateById(attr);
        attrService.updateAttr(attr);
        return R.ok();
    }

    /**
     * 根据spuId修改规格信息
     * 发送请求：
     * 1、参数在路径上，使用@PathVariable注解
     * 2、参数是键值对形式，使用@RequestParam注解
     * 3、参数放到请求体中，且和实体类具有对应关系，使用@RequestBody解析json
     */
    @PostMapping("/update/{spuId}")
    public R updateBySpuId(@PathVariable("spuId") Long spuId,
                           @RequestBody List<ProductAttrValueEntity> entities){
        valueService.updateBySpuId(spuId, entities);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attr:delete")
    public R delete(@RequestBody Long[] attrIds){
		attrService.removeByIds(Arrays.asList(attrIds));
        return R.ok();
    }

}
