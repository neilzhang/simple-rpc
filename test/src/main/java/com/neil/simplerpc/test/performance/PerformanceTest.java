package com.neil.simplerpc.test.performance;

import com.neil.simplerpc.core.client.RpcClient;
import com.neil.simplerpc.core.method.listener.MethodInvocationListener;
import com.neil.simplerpc.core.method.point.MethodInvocationPoint;
import com.neil.simplerpc.core.server.RpcServer;
import com.neil.simplerpc.test.HelloWordService;
import com.neil.simplerpc.test.HelloWordServiceImpl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author neil
 */
public class PerformanceTest {

    private static final int serverThreadNum = 1;
    private static final int clientThreadNum = 20;

    private volatile boolean clientStop = false;
    private long testTime = 60 * 1000;

    private AtomicLong serverFirstTime = new AtomicLong();
    private volatile long serverLastTime = 0;

    private CountDownLatch clientCount = new CountDownLatch(clientThreadNum);
    private CountDownLatch serverCount = new CountDownLatch(serverThreadNum);

    private AtomicLong callCount = new AtomicLong();
    private AtomicLong callCost = new AtomicLong();

    private AtomicLong responseCount = new AtomicLong();
    private AtomicLong responseCost = new AtomicLong();

    public static void main(String[] args) throws InterruptedException {
        new PerformanceTest().runTest();
    }

    private void runTest() throws InterruptedException {
        for (int i = 0; i < serverThreadNum; i++) {
            new ServerThread().start();
        }
        for (int i = 0; i < clientThreadNum; i++) {
            new ClientThread().start();
        }
        Thread.sleep(testTime);
        clientStop = true;
        serverCount.await();
        if (callCount.get() > 0) {
            System.out.println("client call count: `" + callCount.get() + "`.");
            System.out.println("client avg cost: `" + (callCost.get() / callCount.get()) + "` ns.");
        }
        if (responseCount.get() > 0) {
            System.out.println("server response count: `" + responseCount.get() + "`.");
            System.out.println("server avg cost: `" + (responseCost.get() / responseCount.get()) + "` ns.");
            System.out.println("server tps: `" + (responseCount.get() / ((serverLastTime - serverFirstTime.get()) / 1000)));
        }
    }

    class ServerThread extends Thread {

        @Override
        public void run() {
            RpcServer rpcServer = startServer(8998);
            rpcServer.publish(HelloWordService.class, new HelloWordServiceImpl());
            try {
                clientCount.await();
            } catch (InterruptedException e) {
            }
            rpcServer.shutdown();
            serverCount.countDown();
        }

    }

    class ClientThread extends Thread {

        @Override
        public void run() {
            RpcClient rpcClient = startClient();
            HelloWordService helloWordService = rpcClient.proxy(HelloWordService.class);
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (!clientStop) {
                try {
                    helloWordService.sayHello("Neil");
                } catch (Exception e) {
                    System.out.println("client call failed." + e.getMessage());
                }
            }
            rpcClient.close();
            clientCount.countDown();
        }

    }

    private RpcServer startServer(int port) {
        RpcServer rpcServer = RpcServer.builder()
                .port(port)
                .zoo("127.0.0.1:2181")
                .bossThread(1)
                .workThread(4)
                .addListener(new MethodInvocationListener() {
                    @Override
                    public Object around(MethodInvocationPoint point) throws Throwable {
                        serverFirstTime.compareAndSet(0, System.currentTimeMillis());
                        long startNanos = System.nanoTime();
                        Object result = point.proceed();
                        responseCount.incrementAndGet();
                        responseCost.addAndGet(System.nanoTime() - startNanos);
                        serverLastTime = System.currentTimeMillis();
                        return result;
                    }
                })
                .build();
        rpcServer.start();
        return rpcServer;
    }

    private RpcClient startClient() {
        RpcClient rpcClient = RpcClient.builder()
                .zkCoon("127.0.0.1:2181")
                .timeout(3000)
                .addListener(new MethodInvocationListener() {
                    @Override
                    public Object around(MethodInvocationPoint point) throws Throwable {
                        long startNanos = System.nanoTime();
                        Object result = point.proceed();
                        callCount.incrementAndGet();
                        callCost.addAndGet(System.nanoTime() - startNanos);
                        return result;
                    }
                })
                .build();
        rpcClient.init();
        return rpcClient;
    }

}
