package com.neil.simplerpc.core.server;

import com.esotericsoftware.kryo.Kryo;
import com.neil.simplerpc.core.codec.KryoDecoder;
import com.neil.simplerpc.core.codec.KryoEncoder;
import com.neil.simplerpc.core.exception.SimpleRpcException;
import com.neil.simplerpc.core.method.handler.MethodProxyInvocationHandler;
import com.neil.simplerpc.core.method.listener.ServerInvocationListener;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author neil
 */
public class RpcServer {

    private static final String ROOT_PATH = "/simple-rpc";

    private final String host;

    private final int port;

    private final String zkConn;

    private final ServerInvocationListener listener;

    private final Kryo kryo;

    private final ServicePool servicePool;

    private final ServerHandler serverHandler;

    private final CuratorFramework zkClient;

    private final EventLoopGroup bossGroup;

    private final EventLoopGroup workerGroup;

    private List<Class<?>> registeredServices = new ArrayList<>();

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

    public RpcServer(int port, String zkConn, int bossThread, int workThread, ServerInvocationListener listener) {
        try {
            this.host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new SimpleRpcException("");
        }
        this.port = port;
        this.zkConn = zkConn;
        this.listener = listener;
        this.kryo = new Kryo();
        this.servicePool = new ServicePool();
        this.serverHandler = new ServerHandler(servicePool);
        this.bossGroup = new NioEventLoopGroup(bossThread);
        this.workerGroup = new NioEventLoopGroup(workThread);
        this.zkClient = CuratorFrameworkFactory.newClient(zkConn, new ExponentialBackoffRetry(1000, 3));
        this.zkClient.start();
    }

    public static ServerBuilder builder() {
        return new ServerBuilder();
    }

    public void start() {
        if (state == STATE_INITIATED) {
            state = STATE_STARTED;
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

            b.bind(port).channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {

                }
            });
        }
    }

    public void shutdown() {
        if (state == STATE_STARTED) {
            state = STATE_CLOSED;
            for (Class<?> service : registeredServices) {
                unregister(service);
            }
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
        String servicePath = getServicePath(service);
        String target = host + ":" + port;
        try {
            zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(ZKPaths.makePath(servicePath, target));
            registeredServices.add(service);
        } catch (Exception e) {
            // TODO
        }
    }

    private void unregister(Class<?> service) {
        if (registeredServices.remove(service)) {
            String servicePath = getServicePath(service);
            String target = host + ":" + port;
            try {
                zkClient.delete().deletingChildrenIfNeeded().forPath(ZKPaths.makePath(servicePath, target));
            } catch (Exception e) {
                // TODO
            }
        }
    }

    private String getServicePath(Class<?> service) {
        return ROOT_PATH + "/" + service.getName();
    }

}
