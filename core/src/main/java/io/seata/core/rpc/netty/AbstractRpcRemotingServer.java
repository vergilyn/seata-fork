/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.core.rpc.netty;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.seata.common.XID;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.netty.v1.ProtocolV1Decoder;
import io.seata.core.rpc.netty.v1.ProtocolV1Encoder;
import io.seata.discovery.registry.RegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Rpc remoting server.
 *
 * @author slievrly
 * @author xingfudeshi@gmail.com
 */
public abstract class AbstractRpcRemotingServer extends AbstractRpcRemoting implements RemotingServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRpcRemotingServer.class);
    private final ServerBootstrap serverBootstrap;
    private final EventLoopGroup eventLoopGroupWorker;
    private final EventLoopGroup eventLoopGroupBoss;
    private final NettyServerConfig nettyServerConfig;
    private int listenPort;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Sets listen port.
     *
     * @param listenPort the listen port
     */
    public void setListenPort(int listenPort) {

        if (listenPort <= 0) {
            throw new IllegalArgumentException("listen port: " + listenPort + " is invalid!");
        }
        this.listenPort = listenPort;
    }

    /**
     * Gets listen port.
     *
     * @return the listen port
     */
    public int getListenPort() {
        return listenPort;
    }

    /**
     * Instantiates a new Rpc remoting server.
     *
     * @param nettyServerConfig the netty server config
     */
    public AbstractRpcRemotingServer(final NettyServerConfig nettyServerConfig) {
        this(nettyServerConfig, null);
    }

    /**
     * Instantiates a new Rpc remoting server.
     *
     * @param nettyServerConfig the netty server config
     * @param messageExecutor   the message executor
     * @param handlers          the handlers
     */
    public AbstractRpcRemotingServer(final NettyServerConfig nettyServerConfig,
                                     final ThreadPoolExecutor messageExecutor, final ChannelHandler... handlers) {
        super(messageExecutor);
        this.serverBootstrap = new ServerBootstrap();
        this.nettyServerConfig = nettyServerConfig;

        /* vergilyn-comment, 2020-02-13 >>>>
         * netty线程模型（boss、worker）： Reactor模型，参考 https://blog.csdn.net/qq924862077/article/details/81026740
         */
        if (NettyServerConfig.enableEpoll()) {  // vergilyn-comment, 2020-02-13 >>>> linux的epoll
            this.eventLoopGroupBoss = new EpollEventLoopGroup(nettyServerConfig.getBossThreadSize(),
                new NamedThreadFactory(nettyServerConfig.getBossThreadPrefix(), nettyServerConfig.getBossThreadSize()));
            this.eventLoopGroupWorker = new EpollEventLoopGroup(nettyServerConfig.getServerWorkerThreads(),
                new NamedThreadFactory(nettyServerConfig.getWorkerThreadPrefix(),
                    nettyServerConfig.getServerWorkerThreads()));
        } else {
            this.eventLoopGroupBoss = new NioEventLoopGroup(nettyServerConfig.getBossThreadSize(),
                new NamedThreadFactory(nettyServerConfig.getBossThreadPrefix(), nettyServerConfig.getBossThreadSize()));
            this.eventLoopGroupWorker = new NioEventLoopGroup(nettyServerConfig.getServerWorkerThreads(),
                new NamedThreadFactory(nettyServerConfig.getWorkerThreadPrefix(),
                    nettyServerConfig.getServerWorkerThreads()));
        }
        if (null != handlers) {
            channelHandlers = handlers;
        }
        // init listenPort in constructor so that getListenPort() will always get the exact port
        setListenPort(nettyServerConfig.getDefaultListenPort());
    }

    @Override
    public void start() {
        this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupWorker)
            .channel(nettyServerConfig.SERVER_CHANNEL_CLAZZ)
            .option(ChannelOption.SO_BACKLOG, nettyServerConfig.getSoBackLogSize())
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_SNDBUF, nettyServerConfig.getServerSocketSendBufSize())
            .childOption(ChannelOption.SO_RCVBUF, nettyServerConfig.getServerSocketResvBufSize())
            .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                new WriteBufferWaterMark(nettyServerConfig.getWriteBufferLowWaterMark(),
                    nettyServerConfig.getWriteBufferHighWaterMark()))
            .localAddress(new InetSocketAddress(listenPort))
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new IdleStateHandler(nettyServerConfig.getChannelMaxReadIdleSeconds(), 0, 0))
                            .addLast(new ProtocolV1Decoder())
                            .addLast(new ProtocolV1Encoder());
                    if (null != channelHandlers) {
                        addChannelPipelineLast(ch, channelHandlers);
                    }

                }
            });

        if (nettyServerConfig.isEnableServerPooledByteBufAllocator()) {
            this.serverBootstrap.childOption(ChannelOption.ALLOCATOR, NettyServerConfig.DIRECT_BYTE_BUF_ALLOCATOR);
        }

        try {
            ChannelFuture future = this.serverBootstrap.bind(listenPort).sync();
            LOGGER.info("Server started ... ");

            // vergilyn-comment, 2020-02-13 >>>> 将seata-server注册到xxx（e.g. nacos）
            RegistryFactory.getInstance().register(new InetSocketAddress(XID.getIpAddress(), XID.getPort()));
            initialized.set(true);
            future.channel().closeFuture().sync();
        } catch (Exception exx) {
            throw new RuntimeException(exx);
        }

    }

    @Override
    public void shutdown() {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Shutting server down. ");
            }
            if (initialized.get()) {
                RegistryFactory.getInstance().unregister(new InetSocketAddress(XID.getIpAddress(), XID.getPort()));
                RegistryFactory.getInstance().close();

                /* vergilyn-question, 2020-02-16 >>>> 为什么要Thread.sleep一会？
                 *   个人猜测，是为了确保seata-server已经从registry-center中移除，registry-center不会再发送相关请求到当前（准备）关闭的seata-server。
                 */
                //wait a few seconds for server transport
                TimeUnit.SECONDS.sleep(nettyServerConfig.getServerShutdownWaitTime());
            }

            /* vergilyn-comment, 2020-02-17 >>>> 优雅的关闭netty
             *   不管是`EpollEventLoopGroup`还是`NioEventLoopGroup`其默认参数都是(2, 15, SECONDS)
             *   表示，Netty默认在2秒的静默时间内如果没有任务，则关闭；否则15秒截止时间到达时关闭。
             */
            this.eventLoopGroupBoss.shutdownGracefully();
            this.eventLoopGroupWorker.shutdownGracefully();
        } catch (Exception exx) {
            LOGGER.error(exx.getMessage());
        }
    }

    @Override
    public void destroyChannel(String serverAddress, Channel channel) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("will destroy channel:{},address:{}", channel, serverAddress);
        }
        channel.disconnect();
        channel.close();
    }

}
