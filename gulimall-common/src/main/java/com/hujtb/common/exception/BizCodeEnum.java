package com.hujtb.common.exception;

/**
 * 错误码和错误信息定义类
 * 1.错误码定义为5位数字
 * 2.前两位表示业务场景，最后三位表示错误码
 * 3.维护错误码后需要维护错误描述，将他们定义为枚举形式
 * 错误码列表：
 * 10：通用
 * 001：参数格式校验
 * 11：商品
 * 12：订单
 * 13：购物车
 * 14：物流
 * 15：用户
 * 21：库存
 */
public enum BizCodeEnum {
    UNKNOWN_EXCEPTION(10000, "系统为知异常"),
    VALID_EXCEPTION(10001, "参数校验失败"),
    SMS_CODE_EXCEPTION(10002, "验证码获取频率过高，请稍候再试"),
    TOOMANY_REQUEST_EXCEPTION(10003, "流量过大"),
    PRODUCT_UP_EXCEPTION(11000, "商品上架异常"),
    USER_EXIST_EXCEPTION(15001, "用户已存在"),
    PHONE_EXIST_EXCEPTION(15002, "手机号已存在"),
    NO_STOCK_EXCEPTION(21001, "商品库存不足"),
    LOGINACCT_PASSWORD_INVALID_EXCEPTION(15003, "帐号密码错误");

    private Integer code;
    private String msg;

    BizCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return this.getCode();
    }

    public String getMsg() {
        return this.msg;
    }
}
