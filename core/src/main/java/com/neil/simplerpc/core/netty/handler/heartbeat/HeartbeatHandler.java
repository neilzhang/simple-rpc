package com.neil.simplerpc.core.netty.handler.heartbeat;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author neil
 */
public class HeartbeatHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatHandler.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(new HeartbeatMessage());
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HeartbeatMessage) {
            LOGGER.debug("This is a heartbeat message! " + msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private static class HeartbeatMessage {
        @Override
        public String toString() {
            return "♥";
        }
    }

}
