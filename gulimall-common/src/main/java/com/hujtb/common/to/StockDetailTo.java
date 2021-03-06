package com.hujtb.common.to;

import lombok.Data;

@Data
public class StockDetailTo {

    private Long id;
    private Long skuId;
    private String skuName;
    private Integer skuNum;
    private Long taskId;
    private Long wareId;
    private Integer lockStatus;
}
