package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.exception.SimpleRpcException;
import com.neil.simplerpc.core.netty.handler.codec.KryoDecoder;
import com.neil.simplerpc.core.netty.handler.codec.KryoEncoder;
import com.neil.simplerpc.core.netty.handler.heartbeat.HeartbeatHandler;
import com.neil.simplerpc.core.service.ServiceInstance;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author neil
 */
public class ServiceProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProxy.class);

    private ServiceInstance instance;

    private ClientContext clientContext;

    private Channel channel;

    private NioEventLoopGroup group;

    private static final int STATE_LATENT = 0;

    private static final int STATE_ACTIVE = 1;

    private static final int STATE_CLOSED = -1;

    private volatile int state = STATE_LATENT;

    public ServiceProxy(ServiceInstance instance, ClientContext clientContext) {
        this.instance = instance;
        this.clientContext = clientContext;
        this.group = new NioEventLoopGroup(4); // TODO
        this.channel = createChannel(instance.getHost(), instance.getPort());
    }

    public void call(Request request) {
        if (!channel.isActive()) {
            throw new SimpleRpcException("channel is not active. request: `" + request + "`.");
        }
        channel.writeAndFlush(request);
    }

    public void close() {
        if (state != STATE_CLOSED) {
            state = STATE_CLOSED;
            channel.close();
            group.shutdownGracefully();
        }
    }

    public boolean isActive() {
        return state == STATE_ACTIVE && channel.isActive();
    }

    public boolean isConnectionBroken() {
        return state != STATE_LATENT && !isActive();
    }

    public ServiceInstance getServiceInstance() {
        return instance;
    }

    private Channel createChannel(String host, int port) {
        Bootstrap b = new Bootstrap();
        b.group(group);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(new IdleStateHandler(0, 30, 0))
                        .addLast(new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4))
                        .addLast(new LengthFieldPrepender(4))
                        .addLast(new KryoDecoder())
                        .addLast(new KryoEncoder())
                        .addLast(new HeartbeatHandler())
                        .addLast(new ResponseHandler(clientContext));
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                LOGGER.error("ChannelInitializer exception.", cause);
            }
        });

        ChannelFuture channelFuture = b.connect(host, port);
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    state = STATE_ACTIVE;
                }
            }
        });
        Channel channel = channelFuture.channel();
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                close();
            }
        });
        return channel;
    }

}
