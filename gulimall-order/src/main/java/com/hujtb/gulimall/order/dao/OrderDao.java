package com.hujtb.gulimall.order.dao;

import com.hujtb.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单
 * 
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 18:01:13
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

    void updateOrderStatus(@Param("outTradeNo") String out_trade_no, @Param("code") Integer code);
}
