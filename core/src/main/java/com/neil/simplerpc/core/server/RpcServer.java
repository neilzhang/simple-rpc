package com.neil.simplerpc.core.server;

import com.neil.simplerpc.core.exception.SimpleRpcException;
import com.neil.simplerpc.core.method.handler.MethodInvocationHandler;
import com.neil.simplerpc.core.method.listener.MethodInvocationListener;
import com.neil.simplerpc.core.netty.handler.codec.KryoDecoder;
import com.neil.simplerpc.core.netty.handler.codec.KryoEncoder;
import com.neil.simplerpc.core.netty.handler.heartbeat.HeartbeatHandler;
import com.neil.simplerpc.core.registry.provider.ServiceProvider;
import com.neil.simplerpc.core.server.method.point.MethodProxyInvocationPoint;
import com.neil.simplerpc.core.service.ServiceDescriptor;
import com.neil.simplerpc.core.service.ServiceInstance;
import com.neil.simplerpc.core.util.HostUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

import java.lang.reflect.Proxy;

/**
 * @author neil
 */
public class RpcServer {

    private final String host;

    private final int port;

    private final MethodInvocationListener listener;

    private final EventLoopGroup bossGroup;

    private final EventLoopGroup workerGroup;

    private final ServerContext serverContext;

    private ServiceProvider serviceProvider;

    private static final int STATE_CLOSED = -1;

    private static final int STATE_STARTED = 1;

    private static final int STATE_INITIATED = 0;

    /**
     * -1 关闭，0 初始化，1 开启
     */
    private volatile int state = STATE_INITIATED;

    public RpcServer(int port, String zkConn, int bossThread, int workThread) {
        this(port, zkConn, bossThread, workThread, null);
    }

    public RpcServer(int port, String zkConn, int bossThread, int workThread, MethodInvocationListener listener) {
        this.host = HostUtil.getLocalHost();
        this.port = port;
        this.listener = listener;
        this.bossGroup = new NioEventLoopGroup(bossThread);
        this.workerGroup = new NioEventLoopGroup(workThread);
        this.serverContext = new ServerContext();
        if (zkConn != null) {
            this.serviceProvider = new ServiceProvider(zkConn);
        }
    }

    public static ServerBuilder builder() {
        return new ServerBuilder();
    }

    public void start() {
        if (this.state == STATE_INITIATED) {
            this.state = STATE_STARTED;
            final ServerContext context = this.serverContext;
            ServerBootstrap b = new ServerBootstrap();
            b.group(this.bossGroup, this.workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new IdleStateHandler(0, 30, 0))
                                    .addLast(new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4))
                                    .addLast(new LengthFieldPrepender(4))
                                    .addLast(new KryoDecoder())
                                    .addLast(new KryoEncoder())
                                    .addLast(new HeartbeatHandler())
                                    .addLast(new ServiceHandler(context));
                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                            ctx.close();
                        }
                    });

            b.bind(this.port).channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {

                }
            });
            startPublisher();
        }
    }

    public void shutdown() {
        if (state == STATE_STARTED) {
            state = STATE_CLOSED;
            closePublisher();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public void publish(Class<?> service, Object serviceImpl) throws SimpleRpcException {
        MethodProxyInvocationPoint point = new MethodProxyInvocationPoint();
        point.setTarget(serviceImpl);
        Object proxy = Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{service},
                new MethodInvocationHandler(point, this.listener));
        serverContext.registerBean(service, proxy);
        publish(service.getName(), host, port);
    }

    private void startPublisher() {
        if (serviceProvider != null) {
            this.serviceProvider.start();
        }
    }

    private void closePublisher() {
        if (serviceProvider != null) {
            serviceProvider.close();
        }
    }

    private void publish(String service, String host, int port) {
        ServiceDescriptor descriptor = new ServiceDescriptor(service);
        ServiceInstance instance = new ServiceInstance(descriptor, host, port);
        if (serviceProvider != null) {
            serviceProvider.publish(instance);
        }
    }

}
