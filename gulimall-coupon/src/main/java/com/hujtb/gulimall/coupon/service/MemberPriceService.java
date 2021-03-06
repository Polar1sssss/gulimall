package com.hujtb.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.gulimall.coupon.entity.MemberPriceEntity;

import java.util.Map;

/**
 * 商品会员价格
 *
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 18:03:21
 */
public interface MemberPriceService extends IService<MemberPriceEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

