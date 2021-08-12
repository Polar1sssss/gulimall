package com.hujtb.gulimall.ware.vo;

import lombok.Data;

@Data
public class ItemDoneVo {
    private Long itemId;
    private Integer status;
    private String reason;
}
