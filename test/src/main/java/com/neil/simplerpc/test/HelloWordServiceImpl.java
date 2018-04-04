package com.neil.simplerpc.test;

/**
 * @author neil
 */
public class HelloWordServiceImpl implements HelloWordService {

    @Override
    public String sayHello(String name) {
        return "Hello " + name + "!";
    }

}
