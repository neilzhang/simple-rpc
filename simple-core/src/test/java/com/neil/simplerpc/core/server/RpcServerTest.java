package com.neil.simplerpc.core.server;

import com.neil.simplerpc.core.client.RpcClient;
import com.neil.simplerpc.core.method.listener.ClientInvocationListener;
import com.neil.simplerpc.core.method.listener.ServerInvocationListener;
import com.neil.simplerpc.core.method.point.MethodInvocationPoint;
import org.junit.Test;

/**
 * @author neil
 */
public class RpcServerTest {

    @Test
    public void test() {
        RpcServer rpcServer = RpcServer.builder()
                .port(8998)
                .zoo("127.0.0.1:2181")
                .bossThread(1)
                .workThread(4)
                .addListener(new ServerInvocationListener() {
                    @Override
                    public Object around(MethodInvocationPoint point) throws Throwable {
                        // to do something as framework level without any business.
                        // to add monitor.
                        return point.proceed();
                    }
                })
                .build();
        rpcServer.start();

        rpcServer.register(HelloWordService.class, new HelloWordServiceImpl());

        RpcClient rpcClient = RpcClient.builder()
                .zoo("127.0.0.1:2181")
                .timeout(3000)
                .addListener(new ClientInvocationListener() {
                    @Override
                    public Object around(MethodInvocationPoint point) throws Throwable {
                        // to do something as framework level without any business.
                        // to add monitor.
                        return point.proceed();
                    }
                })
                .build();

        HelloWordService helloWordService = rpcClient.proxy(HelloWordService.class);

        helloWordService.sayHello();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            //never happen
        }

        rpcClient.close();
        rpcServer.shutdown();
    }

    interface HelloWordService {

        void sayHello();

    }

    class HelloWordServiceImpl implements HelloWordService {

        @Override
        public void sayHello() {
            System.out.println("Hello World!");
        }

    }
}
