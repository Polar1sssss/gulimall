package com.hujtb.gulimall.order.web;

import com.hujtb.gulimall.order.config.AlipayTemplate;
import com.hujtb.gulimall.order.service.OrderService;
import com.hujtb.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PayWebController {

    @Autowired
    AlipayTemplate alipayTemplate;

    @Autowired
    OrderService orderService;

    @ResponseBody
    @GetMapping(value = "/aliPayOrder", produces = "text/html")
    public String payOrder(@RequestParam("orderSn") String orderSn) {
        PayVo payVo = orderService.getOrderPay(orderSn);
        // pay是一段html代码，即支付页，需要指定返回的类型是
        String pay = alipayTemplate.pay(payVo);
        return pay;
    }
}
