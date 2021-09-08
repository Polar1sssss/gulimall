package com.hujtb.gulimall.product.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main...start...");
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
//        CompletableFuture<Void> async = CompletableFuture.runAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//        }, threadPool);

        // 方法完成后的感知
//        CompletableFuture<Integer> supplyAsync = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }).whenComplete((res, exception) -> {
//            System.out.println("异步任务成功完成，结果是：" + res  + "异常是，" + exception);
//        }).exceptionally(throwable -> {
//            return 10;
//        });

        // 方法完成后的处理
//        CompletableFuture<Integer> supplyAsync = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }).handle((res, throwable) -> {
//            if (res != null) {
//                return res * 2;
//            }
//            if (throwable != null) {
//                return 0;
//            }
//            return 1;
//        });
//        System.out.println("main...end...");

        // 两个任务都执行完，处理另一个任务
//        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("异步任务1");
//            return 10 / 2;
//        }, threadPool);
//        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("异步任务2");
//            return "hello";
//        }, threadPool);
//        CompletableFuture<Void> future3 = future1.runAfterBothAsync(future2, () -> {
//            System.out.println("异步任务3");
//        }, threadPool);
//        CompletableFuture<Void> future4 = future1.thenAcceptBothAsync(future2, (f1, f2) -> {
//            System.out.println("之前的返回结果：" + f1 + ", " + f2);
//            System.out.println("异步任务4");
//        }, threadPool);
//        CompletableFuture<String> future5 = future1.thenCombineAsync(future2, (f1, f2) -> {
//            System.out.println("之前的返回结果：" + f1 + ", " + f2);
//            System.out.println("异步任务5");
//            return f1 + ":" + f2;
//        }, threadPool);

        CompletableFuture<String> futureA = CompletableFuture.supplyAsync(() -> {
            System.out.println("获取A信息...");
            return "hello, A";
        }, threadPool);
        CompletableFuture<String> futureB = CompletableFuture.supplyAsync(() -> {
            System.out.println("获取B信息...");
            return "hello, B";
        }, threadPool);
        CompletableFuture<String> futureC = CompletableFuture.supplyAsync(() -> {
            System.out.println("获取C信息...");
            return "hello, C";
        }, threadPool);
        CompletableFuture<Void> future = CompletableFuture.allOf(futureA, futureB, futureC);
        future.get(); // 阻塞在这，直到所有任务完成
        System.out.println("main...end...");
        CompletableFuture<Object> future1 = CompletableFuture.anyOf(futureA, futureB, futureC);
        future1.get();
    }
}




