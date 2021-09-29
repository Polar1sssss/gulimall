package com.hujtb.gulimall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：
 *      1、@EnableScheduling：允许定时任务
 *      2、@Scheduled：开启一个定时任务
 *      3、自动配置类：TaskSchedulingAutoConfiguration
 * 异步任务：
 *      1、@EnalbeAsync：开启异步任务
 *      2、@Async：给希望异步执行的方法上标注
 *      3、自动配置类：TaskExecutionAutoConfiguration，属性是通过spring.task.execution
 */
@Slf4j
@Component
@EnableAsync
@EnableScheduling
public class HelloSchedule {

    /**
     * 1、Spring中6位组成，不允许第7位的年
     * 2、在周几的位置，1-7代表周一到周日：MON-SUN
     * 3、定时任务不应该阻塞，下一个任务等待上一个任务执行完才能执行
     *    解决阻塞：（1）异步执行（2）支持定时任务线程池，设置 TaskSchedulingProperties，不太好使（3）定时任务异步执行
     * 使用异步任务+定时任务实现定时任务不阻塞的功能
     */
    @Async
    @Scheduled(cron="* * * * * ?")
    public void hello() {
        log.info("hello...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
