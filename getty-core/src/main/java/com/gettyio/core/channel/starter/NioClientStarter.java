/**
 * 包名：org.getty.core.channel.client
 * 版权：Copyright by www.getty.com
 * 描述：
 * 邮箱：189155278@qq.com
 * 时间：2019/9/27
 */
package com.gettyio.core.channel.starter;

import com.gettyio.core.buffer.ChunkPool;
import com.gettyio.core.buffer.Time;
import com.gettyio.core.channel.*;
import com.gettyio.core.channel.SocketChannel;
import com.gettyio.core.channel.config.ClientConfig;
import com.gettyio.core.logging.InternalLogger;
import com.gettyio.core.logging.InternalLoggerFactory;
import com.gettyio.core.pipeline.ChannelPipeline;
import com.gettyio.core.util.ThreadPool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Map;

/**
 * 类名：AioClientStarter.java
 * 描述：Aio客户端
 * 修改人：gogym
 * 时间：2019/9/27
 */
public class NioClientStarter {


    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioClientStarter.class);

    //开启的socket模式 TCP/UDP ,默认tcp
    protected SocketMode socketMode = SocketMode.TCP;
    //客户端服务配置。
    private ClientConfig aioClientConfig = new ClientConfig();
    //aio通道
    private SocketChannel nioChannel;
    //内存池
    private ChunkPool chunkPool;
    //线程池
    private ThreadPool workerThreadPool;

    //责任链对象
    protected ChannelPipeline channelPipeline;

    //Boss线程数，获取cpu核心,核心小于4设置线程为3，大于4设置和cpu核心数一致
    private int bossThreadNum = Runtime.getRuntime().availableProcessors() < 4 ? 3 : Runtime.getRuntime().availableProcessors();
    // Boss共享给Worker的线程数，核心小于4设置线程为1，大于4右移两位
    private int bossShareToWorkerThreadNum = bossThreadNum > 4 ? bossThreadNum >> 2 : bossThreadNum - 2;
    // Worker线程数
    private int workerThreadNum = bossThreadNum - bossShareToWorkerThreadNum;

    /**
     * 简单启动
     *
     * @param host 服务器地址
     * @param port 服务器端口号
     */
    public NioClientStarter(String host, int port) {
        aioClientConfig.setHost(host);
        aioClientConfig.setPort(port);
    }


    /**
     * 配置文件启动
     *
     * @param aioClientConfig 配置
     */
    public NioClientStarter(ClientConfig aioClientConfig) {
        if (null == aioClientConfig.getHost() || "".equals(aioClientConfig.getHost())) {
            throw new NullPointerException("The connection host is null.");
        }
        if (0 == aioClientConfig.getPort()) {
            throw new NullPointerException("The connection port is null.");
        }
        this.aioClientConfig = aioClientConfig;
    }


    /**
     * 设置责任链
     *
     * @param channelPipeline 责任链
     * @return AioClientStarter
     */
    public NioClientStarter channelInitializer(ChannelPipeline channelPipeline) {
        this.channelPipeline = channelPipeline;
        return this;
    }


    public NioClientStarter socketMode(SocketMode socketMode) {
        this.socketMode = socketMode;
        return this;
    }


    /**
     * 设置Boss线程数
     *
     * @param threadNum 线程数
     * @return AioServerStarter
     */
    public NioClientStarter bossThreadNum(int threadNum) {
        this.bossThreadNum = threadNum;
        return this;
    }

    /**
     * 启动客户端。
     *
     * @throws Exception 异常
     */
    public final void start() throws Exception {

        if (this.channelPipeline == null) {
            throw new NullPointerException("The ChannelPipeline is null.");
        }
        //初始化worker线程池
        workerThreadPool = new ThreadPool(ThreadPool.FixedThread, 1);
        //初始化内存池
        chunkPool = new ChunkPool(aioClientConfig.getClientChunkSize(), new Time(), aioClientConfig.isDirect());
        //调用内部启动

        if (socketMode == SocketMode.TCP) {
            startTcp();
        } else {
            startUDP();
        }

    }


    /**
     * 该方法为非阻塞连接。连接成功与否，会回调
     */
    private void startTcp() throws Exception {

        final java.nio.channels.SocketChannel socketChannel = java.nio.channels.SocketChannel.open();
        if (aioClientConfig.getSocketOptions() != null) {
            for (Map.Entry<SocketOption<Object>, Object> entry : aioClientConfig.getSocketOptions().entrySet()) {
                socketChannel.setOption(entry.getKey(), entry.getValue());
            }
        }

        socketChannel.configureBlocking(false);
        /*
         * 连接到指定的服务地址
         */
        socketChannel.connect(new InetSocketAddress(aioClientConfig.getHost(), aioClientConfig.getPort()));

        /*
         * 创建一个事件选择器Selector
         */
        Selector selector = Selector.open();

        /*
         * 将创建的SocketChannel注册到指定的Selector上，并指定关注的事件类型为OP_CONNECT
         */
        socketChannel.register(selector, SelectionKey.OP_CONNECT);


        while (selector.select() > 0) {
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey sk = it.next();
                if (sk.isConnectable()) {
                    java.nio.channels.SocketChannel channel = (java.nio.channels.SocketChannel) sk.channel();
                    //during connecting, finish the connect
                    if (channel.isConnectionPending()) {
                        channel.finishConnect();
                        try {
                            nioChannel = new NioChannel(socketChannel, aioClientConfig, chunkPool, workerThreadNum, channelPipeline);
                            //创建成功立即开始读
                            nioChannel.starRead();
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                            if (nioChannel != null) {
                                closeChannel(socketChannel);
                            }
                        }
                    }
                }
            }
            it.remove();
        }


    }


    private final void startUDP() throws IOException {

        DatagramChannel datagramChannel = DatagramChannel.open();
        datagramChannel.configureBlocking(false);
        Selector selector = Selector.open();
        datagramChannel.register(selector, SelectionKey.OP_READ);
        nioChannel = new UdpChannel(datagramChannel, selector, aioClientConfig, chunkPool, channelPipeline, 2);
        nioChannel.starRead();
    }


    /**
     * 停止客户端
     */
    public final void shutdown() {
        showdown0(false);
    }


    private void showdown0(boolean flag) {
        if (nioChannel != null) {
            nioChannel.close();
            nioChannel = null;
        }
    }

    /**
     * 关闭客户端连接通道
     *
     * @param channel 通道
     */
    private void closeChannel(java.nio.channels.SocketChannel channel) {
        try {
            channel.shutdownInput();
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
        }
        try {
            channel.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取AioChannel
     *
     * @return AioChannel
     */
    public SocketChannel getNioChannel() {
        if (nioChannel != null) {
            if ((nioChannel.getSslHandler()) != null && socketMode != SocketMode.UDP) {
                //如果开启了ssl,要先判断是否已经完成握手
                if (nioChannel.getSslHandler().getSslService().getSsl().isHandshakeCompleted()) {
                    return nioChannel;
                }
                nioChannel.close();
                throw new RuntimeException("The SSL handshcke is not yet complete");
            }
            return nioChannel;
        }
        throw new NullPointerException("AioChannel was null");
    }
}