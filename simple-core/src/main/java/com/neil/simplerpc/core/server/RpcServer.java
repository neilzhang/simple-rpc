package com.neil.simplerpc.core.server;

import com.esotericsoftware.kryo.Kryo;
import com.neil.simplerpc.core.codec.KryoDecoder;
import com.neil.simplerpc.core.codec.KryoEncoder;
import com.neil.simplerpc.core.exception.SimpleRpcException;
import com.neil.simplerpc.core.method.handler.MethodProxyInvocationHandler;
import com.neil.simplerpc.core.method.listener.ServerInvocationListener;
import com.neil.simplerpc.core.proxy.ServicePool;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.lang.reflect.Proxy;

/**
 * @author neil
 */
public class RpcServer {

    private final int port;

    private final String zooConn;

    private final ServerInvocationListener listener;

    private final Kryo kryo;

    private final ServicePool servicePool;

    private final ServerHandler serverHandler;

    private final EventLoopGroup bossGroup;

    private final EventLoopGroup workerGroup;

    private static final int STATE_CLOSED = -1;

    private static final int STATE_STARTED = 1;

    private static final int STATE_INITIATED = 0;

    /**
     * -1 关闭，0 初始化，1 开启
     */
    private volatile int state = STATE_INITIATED;

    public RpcServer(int port, String zooConn, int bossThread, int workThread) {
        this(port, zooConn, bossThread, workThread, null);
    }

    public RpcServer(int port, String zooConn, int bossThread, int workThread, ServerInvocationListener listener) {
        this.port = port;
        this.zooConn = zooConn;
        this.listener = listener;
        this.kryo = new Kryo();
        this.servicePool = new ServicePool();
        this.serverHandler = new ServerHandler(servicePool);
        this.bossGroup = new NioEventLoopGroup(bossThread);
        this.workerGroup = new NioEventLoopGroup(workThread);

    }

    public static ServerBuilder builder() {
        return new ServerBuilder();
    }

    public void start() {
        if (state == STATE_INITIATED) {
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline()
                                        .addLast(new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4))
                                        .addLast(new LengthFieldPrepender(4))
                                        .addLast(new KryoDecoder(kryo))
                                        .addLast(new KryoEncoder(kryo))
                                        .addLast(serverHandler);
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                ctx.close();
                            }
                        });

                try {
                    // Bind and start to accept incoming connections.
                    ChannelFuture f = b.bind(port).sync();

                    registerServer();

                    // Wait until the server socket is closed.
                    // In this example, this does not happen, but you can do that to gracefully
                    // shut down your server.
                    f.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    throw new SimpleRpcException("");
                }
            } finally {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }
    }

    public void shutdown() {
        if (state == STATE_STARTED) {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public void register(Class<?> service, Object serviceImpl) {
        Object proxy = Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{service},
                new MethodProxyInvocationHandler(serviceImpl, listener));
        servicePool.add(service.getName(), proxy);
    }

    private void registerServer() {

    }

}
