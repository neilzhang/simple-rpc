package com.neil.simplerpc.core.server;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.Response;
import com.neil.simplerpc.core.proxy.ServicePool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author neil
 */
public class ServerHandler extends SimpleChannelInboundHandler<Request> {

    private final ServicePool servicePool;

    private ConcurrentHashMap<String, Method> methodMap = new ConcurrentHashMap<>();

    public ServerHandler(ServicePool servicePool) {
        this.servicePool = servicePool;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request msg) throws Exception {
        String serviceName = msg.getServiceName();
        String methodName = msg.getMethodName();
        Class<?>[] parameterTypes = msg.getParameterTypes();
        Object service = servicePool.getService(serviceName);
        if (service == null) {
            // TODO add logger
            return;
        }
        Method method = getMethod(serviceName, methodName, parameterTypes);
        if (method == null) {
            // TODO add logger
            return;
        }
        Response response = new Response();
        response.setRequestId(msg.getRequestId());
        Object[] params = msg.getParameters();
        method.setAccessible(true);
        try {
            Object result = method.invoke(service, params);
            response.setData(result);
        } catch (InvocationTargetException e) {
            response.setThrowable(e.getCause());
        }
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
    }

    private Method getMethod(String serviceName, String methodName, Class<?>[] parameterTypes) {
        Object service = servicePool.getService(serviceName);
        if (service == null) {
            return null;
        }
        String signature = getSignature(serviceName, methodName, parameterTypes);
        Method method = methodMap.get(signature);
        if (method != null) {
            return method;
        }
        try {
            method = service.getClass().getDeclaredMethod(methodName, parameterTypes);
            methodMap.put(signature, method);
        } catch (NoSuchMethodException e) {
            // TODO add logger
        }
        return method;
    }

    private String getSignature(String serviceName, String methodName, Class<?>[] parameterTypes) {
        StringBuilder signature = new StringBuilder();
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
