package com.neil.simplerpc.core.server;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author neil
 */
public class ServiceHandler extends SimpleChannelInboundHandler<Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceHandler.class);

    private final ServerContext serverContext;

    public ServiceHandler(ServerContext serverContext) {
        this.serverContext = serverContext;
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
        } else {
            Method method = null;
            try {
                method = serverContext.getMethod(bean, methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                // TODO add logger
                response.setThrowable(new Throwable(""));
            }
            if (method != null) {
                try {
                    method.setAccessible(true);
                    Object result = method.invoke(bean, params);
                    response.setData(result);
                } catch (InvocationTargetException e) {
                    response.setThrowable(e.getCause());
                }
            }
        }
        writeResponse(ctx, response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("ServiceHandler exception.", cause);
    }

    private void writeResponse(ChannelHandlerContext ctx, Response response) {
        ctx.writeAndFlush(response);
    }

}
