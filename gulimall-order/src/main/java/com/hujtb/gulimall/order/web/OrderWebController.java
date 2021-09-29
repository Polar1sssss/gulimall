package com.hujtb.gulimall.order.web;

import com.hujtb.gulimall.order.service.OrderService;
import com.hujtb.gulimall.order.vo.OrderConfirmVo;
import com.hujtb.gulimall.order.vo.OrderSubmitResponseVo;
import com.hujtb.gulimall.order.vo.OrderSubmitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    /**
     * 前往结算页
     *
     * @return
     */
    @GetMapping("/toTrade")
    public String toTrade(Model model) {
        OrderConfirmVo confirmVo = orderService.getConfirm();
        model.addAttribute("confirmOrderData", confirmVo);
        return "confirm";
    }

    /**
     * 下单功能
     *
     * @param submitVo
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo submitVo, Model model, RedirectAttributes redirectAttributes) {
        OrderSubmitResponseVo responseVo = orderService.submitOrder(submitVo);
        Integer code = responseVo.getCode();
        if (code == 0) {
            // 下单成功来到支付选择页
            model.addAttribute("submitOrderResp", responseVo);
            return "pay";
        } else {
            String msg = "下单失败：";
            switch (code) {
                case 1:
                    msg += "订单信息过期，请刷新后再提交";
                    break;
                case 2:
                    msg += "订单商品价格发生变化，请确认后再提交";
                    break;
                case 3:
                    msg += "商品库存不足";
                    break;
            }
            redirectAttributes.addFlashAttribute("msg", msg);
            // 下单失败回到订单确认页
            return "redirect:http://order.gulimall.com/confirm.html";
        }
    }
}
