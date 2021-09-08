package com.hujtb.gulimall.member;

import org.apache.commons.codec.digest.Md5Crypt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class GulimallMemberApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testMD5() {
        String s = Md5Crypt.md5Crypt("123456".getBytes(), "$1$qqqqqqqq");
        System.out.println(s);

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode("123456");
        boolean matches = passwordEncoder.matches("123456", "");
        System.out.println(encode + "=>" + matches);
    }

}
