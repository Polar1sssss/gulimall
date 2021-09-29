package com.hujtb.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareVo {
    MemberAddressVo addressVo;
    BigDecimal fare;
}
