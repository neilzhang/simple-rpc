package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端处理器
 * <p>接收服务端返回的 Response </p>
 *
 * @author neil
 */
public class ResponseHandler extends SimpleChannelInboundHandler<Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseHandler.class);

    /**
     * 客户端上下文
     */
    private final ClientContext clientContext;

    /**
     * 构造客户端处理器
     *
     * @param clientContext 客户端上下文
     */
    public ResponseHandler(ClientContext clientContext) {
        this.clientContext = clientContext;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response msg) throws Exception {
        clientContext.receiveResponse(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("ResponseHandler exception.", cause);
    }

}
