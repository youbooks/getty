package com.gettyio.string.nio;


import com.gettyio.core.channel.SocketChannel;
import com.gettyio.core.pipeline.in.SimpleChannelInboundHandler;

import java.io.IOException;

public class SimpleHandler extends SimpleChannelInboundHandler<String> {
    @Override
    public void channelAdded(SocketChannel aioChannel) {

        try {
            System.out.println("连接成功"+aioChannel.getRemoteAddress().getHostString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void channelClosed(SocketChannel aioChannel) {
        System.out.println("连接关闭了");
    }


    @Override
    public void channelRead0(SocketChannel aioChannel, String str) {
        System.out.println("读取消息了:" + str);
    }

    @Override
    public void exceptionCaught(SocketChannel aioChannel, Throwable cause) throws Exception {
        System.out.println("出错了");
    }
}
