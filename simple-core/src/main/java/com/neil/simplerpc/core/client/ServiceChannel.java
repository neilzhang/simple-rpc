package com.neil.simplerpc.core.client;

import com.esotericsoftware.kryo.Kryo;
import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.codec.KryoDecoder;
import com.neil.simplerpc.core.codec.KryoEncoder;
import com.neil.simplerpc.core.exception.SimpleRpcException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author neil
 */
public class ServiceChannel {

    private static final String ROOT_PATH = "/simple-rpc";

    private final AtomicLong count = new AtomicLong(0);

    private final ExecutorService recoverService = Executors.newSingleThreadExecutor();

    private final ExecutorService zkListenerExecutor = Executors.newFixedThreadPool(2);

    private final CopyOnWriteArrayList<ChannelWrapper> channelWrapperList = new CopyOnWriteArrayList<>();

    private Class<?> service;

    private Kryo kryo;

    private ClientHandler clientHandler;

    public ServiceChannel(Class<?> service, CuratorFramework zkClient) {
        this.service = service;
        this.kryo = new Kryo();
        this.clientHandler = new ClientHandler();

        String servicePath = getServicePath(service);
        PathChildrenCache childrenCache = new PathChildrenCache(zkClient, servicePath, true);
        childrenCache.getListenable().addListener(
                new PathChildrenCacheListener() {
                    @Override
                    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event)
                            throws Exception {
                        String target;
                        switch (event.getType()) {
                            case CHILD_ADDED:
                                target = new String(event.getData().getData());
                                channelWrapperList.add(new ChannelWrapper(target));
                                break;
                            case CHILD_REMOVED:
                                target = new String(event.getData().getData());
                                for (ChannelWrapper channelWrapper : channelWrapperList) {
                                    if (channelWrapper.getTarget().equals(target)) {
                                        channelWrapperList.remove(channelWrapper);
                                    }
                                }
                                break;
                            case CHILD_UPDATED:
                                target = new String(event.getData().getData());
                                for (ChannelWrapper channelWrapper : channelWrapperList) {
                                    if (channelWrapper.getTarget().equals(target)) {
                                        channelWrapperList.remove(channelWrapper);
                                    }
                                }
                                channelWrapperList.add(new ChannelWrapper(target));
                                break;
                            case INITIALIZED:
                                List<ChildData> initialData = event.getInitialData();
                                for (ChildData childData : initialData) {
                                    target = new String(childData.getData());
                                    channelWrapperList.add(new ChannelWrapper(target));
                                }
                                break;
                            default:
                                break;
                        }
                    }
                },
                zkListenerExecutor
        );
        try {
            childrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        } catch (Exception e) {
            // TODO
        }
    }

    public void call(Request request) {
        ChannelWrapper channel = selectChannel();
        if (channel == null) {
            throw new SimpleRpcException("there is no available chanel. Request: `" + request + "`.");
        }
        channel.send(request);
    }

    public void close() {
        for (ChannelWrapper channel : channelWrapperList) {
            channel.close();
        }
    }

    private ChannelWrapper selectChannel() {
        ChannelWrapper channel = null;
        while (!channelWrapperList.isEmpty()) {
            int index = (int) Math.abs(count.incrementAndGet() % channelWrapperList.size());
            channel = channelWrapperList.get(index);
            if (channel.isActive()) {
                break;
            } else {
                recoverService.submit(new RecoverTask(channel));
                continue;
            }
        }
        return channel;
    }

    private String getServicePath(Class<?> service) {
        return ROOT_PATH + "/" + service.getName();
    }

    private class ChannelWrapper {

        private String target;

        private Channel channel;

        private EventLoopGroup workerGroup = new NioEventLoopGroup();

        public ChannelWrapper(String target) {
            this.target = target;
            this.channel = createChannel(target);
        }

        public String getTarget() {
            return target;
        }

        public Channel getChannel() {
            return channel;
        }

        public boolean isActive() {
            return channel.isActive();
        }

        public void close() {
            if (channel != null) {
                channel.close();
                channel = null;
            }
            workerGroup.shutdownGracefully();
        }

        public void send(Object msg) {
            channel.writeAndFlush(msg);
        }

        private Channel createChannel(String target) {
            String ip;
            int port;
            try {
                String[] partArr = target.split(":");
                ip = partArr[0];
                port = Integer.valueOf(partArr[1]);
            } catch (NumberFormatException e) {
                throw new SimpleRpcException("");
            }

            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                            .addLast(new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4))
                            .addLast(new LengthFieldPrepender(4))
                            .addLast(new KryoDecoder(kryo))
                            .addLast(new KryoEncoder(kryo))
                            .addLast(clientHandler);
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                    ctx.close();
                }
            });

            Channel channel = b.bind(ip, port).channel();
            channel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {

                }
            });
            return channel;
        }

    }

    public class RecoverTask implements Runnable {

        private ChannelWrapper channel;

        public RecoverTask(ChannelWrapper channel) {
            this.channel = channel;
        }

        @Override
        public void run() {
            try {
                channel.close();
                channelWrapperList.remove(channel);
                channelWrapperList.add(new ChannelWrapper(channel.getTarget()));
            } catch (Exception e) {
                // TODO
            }
        }

    }
}
