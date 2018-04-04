package com.neil.simplerpc.test;

import com.neil.simplerpc.core.client.RpcClient;
import com.neil.simplerpc.core.method.listener.ClientInvocationListener;
import com.neil.simplerpc.core.method.point.MethodInvocationPoint;

import java.util.Scanner;

/**
 * @author neil
 */
public class RpcClientTest {

    public static void main(String[] args) {
        RpcClient rpcClient = null;
        HelloWordService helloWordService = null;
        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            String cmd = sc.nextLine();
            switch (cmd) {
                case "start":
                    rpcClient = RpcClient.builder()
                            .zkCoon("127.0.0.1:2181")
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
                    rpcClient.init();
                    helloWordService = rpcClient.proxy(HelloWordService.class);
                    System.out.println("started");
                    break;
                case "stop":
                    if (rpcClient != null) {
                        rpcClient.close();
                    }
                    System.out.println("stopped");
                    return;
                case "call":
                    if (rpcClient != null) {
                        try {
                            System.out.println(helloWordService.sayHello("neil"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    }

}
