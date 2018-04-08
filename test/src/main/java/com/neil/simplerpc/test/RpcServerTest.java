package com.neil.simplerpc.test;

import com.neil.simplerpc.core.method.listener.MethodInvocationListener;
import com.neil.simplerpc.core.method.point.MethodInvocationPoint;
import com.neil.simplerpc.core.server.RpcServer;

import java.util.Scanner;

/**
 * @author neil
 */
public class RpcServerTest {

    public static void main(String[] args) {
        RpcServer rpcServer = RpcServer.builder()
                .port(8998)
                .zoo("127.0.0.1:2181")
                .bossThread(1)
                .workThread(4)
                .addListener(new MethodInvocationListener() {
                    @Override
                    public Object around(MethodInvocationPoint point) throws Throwable {
                        // to do something as framework level without any business.
                        // to add monitor.
                        return point.proceed();
                    }
                })
                .build();
        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            String cmd = sc.nextLine();
            switch (cmd) {
                case "start":
                    rpcServer.start();
                    System.out.println("started");
                    break;
                case "stop":
                    rpcServer.shutdown();
                    System.out.println("stopped");
                    return;
                case "publish":
                    rpcServer.publish(HelloWordService.class, new HelloWordServiceImpl());
                    System.out.println("published");
                    break;
            }
        }
    }

}
