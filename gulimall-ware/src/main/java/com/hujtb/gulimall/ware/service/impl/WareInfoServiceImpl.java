package com.hujtb.gulimall.ware.service.impl;

import com.hujtb.common.utils.R;
import com.hujtb.gulimall.ware.feign.MemberFeignService;
import com.hujtb.gulimall.ware.vo.FareVo;
import com.hujtb.gulimall.ware.vo.MemberAddressVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.Query;

import com.hujtb.gulimall.ware.dao.WareInfoDao;
import com.hujtb.gulimall.ware.entity.WareInfoEntity;
import com.hujtb.gulimall.ware.service.WareInfoService;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> queryWrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)) {
            queryWrapper.eq("id", key)
                    .or().like("name", key)
                    .or().like("address", key)
                    .or().like("areacode", key);
        }

        IPage<WareInfoEntity> page = this.page(new Query<WareInfoEntity>().getPage(params), queryWrapper);
        return new PageUtils(page);
    }

    @Override
    public FareVo getFare(Long addrId) {
        FareVo fareVo = new FareVo();
        R addrInfo = memberFeignService.addrInfo(addrId);
        if (addrInfo != null) {
            MemberAddressVo addressVo = (MemberAddressVo) addrInfo.get("memberReceiveAddress");
            String phone = addressVo.getPhone();
            String fare = phone.substring(phone.length() - 1);
            fareVo.setAddressVo(addressVo);
            fareVo.setFare(new BigDecimal(fare));
            return fareVo;
        }
        return null;
    }

}