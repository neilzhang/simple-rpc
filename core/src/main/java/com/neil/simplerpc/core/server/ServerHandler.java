package com.neil.simplerpc.core.server;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author neil
 */
public class ServerHandler extends SimpleChannelInboundHandler<Request> {

    private final ServerContext serverContext;

    private final ConcurrentHashMap<String, Method> methodMap;

    private final Object methodMapLock = new Object();

    public ServerHandler(ServerContext serverContext) {
        this.serverContext = serverContext;
        this.methodMap = new ConcurrentHashMap<>();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request msg) throws Exception {
        Response response = new Response();
        response.setRequestId(msg.getRequestId());
        String serviceName = msg.getServiceName();
        String methodName = msg.getMethodName();
        Object[] params = msg.getParameters();
        Class<?>[] parameterTypes = msg.getParameterTypes();
        Object bean = serverContext.getBean(serviceName);
        if (bean == null) {
            // TODO add logger
            response.setThrowable(new Throwable(""));
            writeResponse(ctx, response);
            return;
        }
        Method method = cacheAndGetMethod(bean, methodName, parameterTypes);
        if (method == null) {
            // TODO add logger
            response.setThrowable(new Throwable(""));
            writeResponse(ctx, response);
            return;
        }
        try {
            method.setAccessible(true);
            Object result = method.invoke(bean, params);
            response.setData(result);

        } catch (InvocationTargetException e) {
            response.setThrowable(e.getCause());
        }
        writeResponse(ctx, response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    private void writeResponse(ChannelHandlerContext ctx, Response response) {
        ctx.writeAndFlush(response);
    }

    private Method cacheAndGetMethod(Object bean, String methodName, Class<?>[] parameterTypes) {
        String signature = getSignature(bean, methodName, parameterTypes);
        Method method = methodMap.get(signature);
        if (method == null) {
            synchronized (methodMapLock) {
                method = methodMap.get(signature);
                if (method == null) {
                    try {
                        method = bean.getClass().getDeclaredMethod(methodName, parameterTypes);
                        methodMap.put(signature, method);
                    } catch (NoSuchMethodException e) {
                        // TODO add logger
                        e.printStackTrace();
                    }
                }
            }
        }
        return method;
    }

    private String getSignature(Object bean, String methodName, Class<?>[] parameterTypes) {
        StringBuilder signature = new StringBuilder();
        String serviceName = bean.getClass().getName();
        signature.append(serviceName);
        signature.append('.');
        signature.append(methodName);
        signature.append('(');
        if (parameterTypes != null) {
            for (Class<?> parameterType : parameterTypes) {
                signature.append(parameterType.getName());
                if (!parameterType.equals(parameterTypes[parameterTypes.length - 1])) {
                    signature.append(',');
                }
            }
        }
        signature.append(')');
        return signature.toString();
    }

}
