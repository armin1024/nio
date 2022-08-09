package cn.arminxss.msb.netty;

import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MyNetty {

    @Test
    public void myByteBuf() {
//        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(8, 20); // isDirect true
//        ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.heapBuffer(8, 20); // un pool isDirect false
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.heapBuffer(8, 20); // pool
        print(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        print(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        print(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        print(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        print(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        print(buf);
//        buf.writeBytes(new byte[]{1,2,3,4});
//        print(buf);
    }

    private static void print(ByteBuf buf) {
        System.out.println("buf.isReadable()    : " + buf.isReadable()); // 是否可读
        System.out.println("buf.readerIndex()   : " + buf.readerIndex()); // 读起始索引
        System.out.println("buf.readableBytes() : " + buf.readableBytes()); // 可以读多少
        System.out.println("buf.isWritable()    : " + buf.isWritable()); // 是否可写
        System.out.println("buf.writerIndex()   : " + buf.writerIndex()); // 从索引位开始写
        System.out.println("buf.writableBytes() : " + buf.writableBytes()); // 可写多少
        System.out.println("buf.capacity()      : " + buf.capacity()); // buf当前上线大小（动态分配）
        System.out.println("buf.maxCapacity()   : " + buf.maxCapacity()); // buf上线大小
        System.out.println("buf.isDirect()      : " + buf.isDirect()); // 是否堆外分配
        System.out.println("------------------------");
    }

    @Test
    public void loopExecutor() throws IOException {
        NioEventLoopGroup selector = new NioEventLoopGroup(2); // 定义线程数
        selector.execute(() -> {
            try {
                while (true) {
                    System.out.println("No.1 hello netty!");
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        selector.execute(() -> {
            try {
                while (true) {
                    System.out.println("No.2 hello netty!");
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.in.read(); // 此方法会一直阻塞，直到输入数据可用
    }

    @Test
    public void clientMode() throws InterruptedException {
        NioEventLoopGroup thread = new NioEventLoopGroup(1);
        // 客户端模式
        NioSocketChannel client = new NioSocketChannel();
        thread.register(client);
        // 响应式
        ChannelPipeline p = client.pipeline();
        p.addLast(new MyInHandler());

        // reactor 异步特征
        ChannelFuture connect = client.connect(new InetSocketAddress("192.168.137.229", 8888));
        ChannelFuture sync = connect.sync(); // 同步链接

        ByteBuf buf = Unpooled.copiedBuffer("hello server".getBytes());
        ChannelFuture send = client.writeAndFlush(buf);
        send.sync(); // 同步发送

        sync.channel().closeFuture().sync(); // 同步关闭客户端
        System.out.println("client over...");
    }

    @Test
    public void serverMode() throws InterruptedException {
        NioEventLoopGroup thread = new NioEventLoopGroup(1);
        NioServerSocketChannel server = new NioServerSocketChannel();

        thread.register(server);

        ChannelPipeline p = server.pipeline();
        p.addLast(new MyAcceptHandler(thread, new ChannelInit()));
        ChannelFuture bind = server.bind(new InetSocketAddress("192.168.137.229", 8888));

        bind.sync().channel().closeFuture().sync();
        System.out.println("server closed...");
    }

}

// 可使用抽象类定义泛型，可使用户自定义自己的handler
@ChannelHandler.Sharable
class ChannelInit extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Channel client = ctx.channel();
        ChannelPipeline p = client.pipeline();
        p.addLast(new MyInHandler()); // 2,client::pipeline[ChannelInit,MyInHandler]
        ctx.pipeline().remove(this); // 过河拆桥，注册后该方法对象可移除 ChannelInit仅用于初始化，初始化完成后即可移除
    }
}

class MyAcceptHandler extends ChannelInboundHandlerAdapter {

    private final EventLoopGroup selector;
    private final ChannelHandler handler;

    public MyAcceptHandler(EventLoopGroup thread, ChannelHandler handler) {
        this.selector = thread;
        this.handler = handler;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("server registered");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        SocketChannel client = (SocketChannel) msg;
        ChannelPipeline p = client.pipeline();
        p.addLast(handler); // 1,client::pipeline[ChannelInit,]

        selector.register(client);
    }

}

//@ChannelHandler.Sharable // 多线程共享此handler，该handler被多个线程重复注册
class MyInHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client registered");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client active");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
//        CharSequence str = buf.readCharSequence(buf.readableBytes(), CharsetUtil.UTF_8); // read会改变buf中的指针
        CharSequence str = buf.getCharSequence(0, buf.readableBytes(), CharsetUtil.UTF_8); // get不会改变buf中的指针
        System.out.println(str);
        ctx.writeAndFlush(buf); // 响应服务端数据
    }
}
