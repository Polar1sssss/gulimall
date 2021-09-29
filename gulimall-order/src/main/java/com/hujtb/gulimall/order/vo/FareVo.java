package com.hujtb.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareVo {
    MemberAddressVo addressVo;
    BigDecimal fare;
}
