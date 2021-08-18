package com.hujtb.gulimall.product.web;

import com.hujtb.gulimall.product.entity.CategoryEntity;
import com.hujtb.gulimall.product.service.CategoryService;
import com.hujtb.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redisson;

    @Autowired
    StringRedisTemplate redisTemplate;

    @GetMapping({"/", "/index.html"})
    public String IndexPage(Model model) {
        List<CategoryEntity> entityList = categoryService.getLevel1Categories();
        model.addAttribute("categories", entityList);
        return "index";
    }

    /**
     * 查询二三级分类的json
     *
     * @return
     */
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        Map<String, List<Catalog2Vo>> catalogJson = categoryService.getCatalogJson();
        return catalogJson;
    }

    /**
     * 保证一定能读到最新的数据，修改期间，写锁是一个排它锁（互斥锁），读锁是一个共享锁
     * 读 + 读：相当于无锁，并发读只会在redis中记录所有锁的状态，会同时加读锁成功
     * 写 + 读：需要等待写锁释放才能读
     * 写 + 写：阻塞方式
     * 读 + 写：需要等待读锁释放才能写
     */
    @RequestMapping("/write")
    @ResponseBody
    public String writeValue() {
        String s = UUID.randomUUID().toString();
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        RLock writeLock = readWriteLock.writeLock();
        try {
            writeLock.lock();
            Thread.sleep(30000);
            redisTemplate.opsForValue().set("writeValue", s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
        return "ok";
    }

    @RequestMapping("/read")
    @ResponseBody
    public String readValue() {
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        RLock readLock = readWriteLock.readLock();
        try {
            readLock.lock();
            redisTemplate.opsForValue().get("writeValue");
        } finally {
            readLock.unlock();
        }
        return "ok";
    }

    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore semaphore = redisson.getSemaphore("park");
        semaphore.acquire(); // 获取一个信号量
        return "ok";
    }

    @GetMapping("/go")
    @ResponseBody
    public String go() {
        RSemaphore semaphore = redisson.getSemaphore("park");
        semaphore.release();
        return "ok";
    }

    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.trySetCount(5);
        door.await(); // 等待闭锁都完成
        return "放假了...";
    }

    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id) {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.countDown();
        return id + "班离开学校";
    }
}
