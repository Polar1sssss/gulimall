package com.hujtb.common.constant;

public class ProductConst {
    public enum AttrConst {
        ATTR_BASE(1, "基本属性"), ATTR_SALE(0, "销售属性");
        private int code;
        private String msg;

        AttrConst(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }

    public enum StatusEnum {
        NEW(0, "新建"), UP(1, "上架"), DOWN(2, "下架");
        private int code;
        private String msg;

        StatusEnum(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}
