package com.hujtb.gulimall.ssoclient.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试单点登录
 */
@Controller
public class HelloController {

    @Value("${sso.server.url}")
    private String url;

    /**
     * 无需登录就可访问
     *
     * @return
     */
    @ResponseBody
    @GetMapping(value = "/hello")
    public String hello() {
        return "hello";
    }


    @GetMapping(value = "/employees")
    public String employees(Model model, HttpSession session, @RequestParam(name = "token", required = false) String token) {
        List<String> emps = new ArrayList<>();
        // 如果有一个客户端登录成功，就会生成一个token，并在服务端的cookie中保存此token
        if (!StringUtils.isEmpty(token)) {
            // 从登录服务器获取的用户信息
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> forEntity = restTemplate.getForEntity("http://ssoserver.com:8080/userInfo?token=" + token, String.class);
            String body = forEntity.getBody();
            session.setAttribute("loginUser", body);
        }
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            //没登录,跳转到服务器登录
            return "redirect:" + url + "?redirect_url=http://client1.com:8081/employees";
        } else {
            emps.add("张三");
            emps.add("李四");
            model.addAttribute("emps", emps);
            return "employees";
        }

    }
}

