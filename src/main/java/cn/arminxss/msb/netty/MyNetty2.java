package cn.arminxss.msb.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.net.InetSocketAddress;

public class MyNetty2 {

    @Test
    public void clientMode() throws InterruptedException {
        NioEventLoopGroup thread = new NioEventLoopGroup(1);
        NioSocketChannel client = new NioSocketChannel();

        thread.register(client);
        ChannelPipeline p = client.pipeline();
        p.addLast(new MyInHandler2());

        ChannelFuture connect = client.connect(new InetSocketAddress("192.168.137.229", 8888));
        ChannelFuture sync = connect.sync(); // 同步注册

        ByteBuf buf = Unpooled.copiedBuffer("client registered".getBytes());
        ChannelFuture send = client.writeAndFlush(buf);
        send.sync(); // 同步发送

        sync.channel().closeFuture().sync(); // 同步关闭
        System.out.println("server closed..");
    }

    @Test
    public void serverMode() throws InterruptedException {
        NioEventLoopGroup thread = new NioEventLoopGroup(1);
        NioServerSocketChannel server = new NioServerSocketChannel();
        thread.register(server);

        ChannelPipeline p = server.pipeline();
        p.addLast(new MyAcceptHandler2(thread, new ChannelInit2()));

        ChannelFuture bind = server.bind(new InetSocketAddress("192.168.31.224", 8888));
        bind.sync().channel().closeFuture().sync(); // 同步关闭

        System.out.println("server closed.");
    }

    @Test
    public void nettyClient() throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bs = new Bootstrap();
        ChannelFuture connect = bs.group(group)
                .channel(NioSocketChannel.class)
//                .handler(new ChannelInitializer<SocketChannel>() {
//                    @Override
//                    protected void initChannel(SocketChannel socketChannel) throws Exception {
//                        ChannelPipeline p = socketChannel.pipeline();
//                        p.addLast(new MyInHandler2());
//                    }
//                })
                .handler(new ChannelInit2())
                .connect(new InetSocketAddress("localhost", 9999));

        Channel client = connect.sync().channel(); // 同步链接并获取到channel
        ByteBuf buf = Unpooled.copiedBuffer(("client: " + Thread.currentThread().getName()).getBytes());
        ChannelFuture send = client.writeAndFlush(buf);
        send.sync(); // 同步发送

        client.closeFuture().sync(); // 客户端同步关闭
    }

    @Test
    public void nettyServer() throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        ChannelFuture bind = new ServerBootstrap()
                .group(group, group) // 两个group其中一个类似serverMode中的MyAcceptHandler selector
                .channel(NioServerSocketChannel.class)
//                .handler(new ChannelInit2())
                .childHandler(new ChannelInitializer<SocketChannel>() { // 仅需服务端注册childHandler，group(group,group)已处理AcceptHandler
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline p = socketChannel.pipeline();
                        p.addLast(new MyInHandler2());
                    }
                })
                .bind(new InetSocketAddress("192.168.31.224", 9999));
        bind.sync().channel().closeFuture().sync(); // server 同步注册与关闭
    }

}

@ChannelHandler.Sharable
class ChannelInit2 extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Channel client = ctx.channel();
        ChannelPipeline p = client.pipeline();
        p.addLast(new MyInHandler());
        ctx.pipeline().remove(this); // ChannelInit仅用于初始化，初始化完成后即可移除
    }
}

class MyAcceptHandler2 extends ChannelInboundHandlerAdapter {

    private EventLoopGroup selector;
    private ChannelHandler handler;

    public MyAcceptHandler2(EventLoopGroup thread, ChannelHandler handler) {
        this.selector = thread;
        this.handler = handler;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("server registered.");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("server active.");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel client = (Channel) msg;
        ChannelPipeline p = client.pipeline();
        p.addLast(handler);
        selector.register(client);
    }
}

class MyInHandler2 extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client registered.");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client active.");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        CharSequence str = buf.getCharSequence(0, buf.readableBytes(), CharsetUtil.UTF_8);
        ctx.writeAndFlush(msg);
        System.out.println(str);
    }
}

