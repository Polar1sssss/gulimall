package com.hujtb.gulimall.product.vo;

import lombok.Data;

@Data
public class AttrResponseVo extends AttrVo{
    private String catalogName;
    private String attrGroupName;
    private Long[] catalogPath;
}
