package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author neil
 */
public class ClientHandler extends SimpleChannelInboundHandler<Response> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response msg) throws Exception {
        ResponseShelf.place(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.channel().close();
    }

}
