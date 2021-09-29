package com.hujtb.gulimall.order.vo;

import com.hujtb.gulimall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class OrderSubmitResponseVo {

    private OrderEntity order;
    // 返回码，0代表成功，1代表失败
    private Integer code;
}
