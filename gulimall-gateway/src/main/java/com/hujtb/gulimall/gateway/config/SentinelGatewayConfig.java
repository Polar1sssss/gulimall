package com.hujtb.gulimall.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.fastjson.JSON;
import com.hujtb.common.exception.BizCodeEnum;
import com.hujtb.common.utils.R;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SentinelGatewayConfig {

   public SentinelGatewayConfig() {
       GatewayCallbackManager.setBlockHandler(new BlockRequestHandler() {
            // 违反限流规则 就会自动调用此回调
           @Override
           public Mono<ServerResponse> handleRequest(ServerWebExchange serverWebExchange, Throwable throwable) {
               R error = R.error(BizCodeEnum.TOOMANY_REQUEST_EXCEPTION.getCode(), BizCodeEnum.TOOMANY_REQUEST_EXCEPTION.getMsg());
               String errJson = JSON.toJSONString(error);
               Mono<ServerResponse> body = ServerResponse.ok().body(Mono.just(errJson), String.class);
               return body;
           }
       });
   }
}
