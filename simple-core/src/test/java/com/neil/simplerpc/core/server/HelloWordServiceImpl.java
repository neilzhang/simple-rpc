package com.neil.simplerpc.core.server;

/**
 * @author neil
 */
public class HelloWordServiceImpl implements HelloWordService {

    @Override
    public void sayHello() {
        System.out.println("Hello World!");
    }

}
