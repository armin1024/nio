package cn.arminxss.msb.rpcdemo;

import cn.arminxss.msb.rpcdemo.proxy.MyProxy;
import cn.arminxss.msb.rpcdemo.rpc.protocal.MyContent;
import cn.arminxss.msb.rpcdemo.rpc.transport.MyHttpRpcHandler;
import cn.arminxss.msb.rpcdemo.rpc.transport.ServerDecode;
import cn.arminxss.msb.rpcdemo.rpc.transport.ServerRequestHandler;
import cn.arminxss.msb.rpcdemo.service.*;
import cn.arminxss.msb.rpcdemo.util.SerUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class MyRpcTest {

    @Test
    public void startHttpServer() {
        // tomcat jetty  【servlet】
        MyCar car = new MyCar();
        Dispatcher dispatcher = Dispatcher.getInstance();
        dispatcher.register(Car.class.getName(), car);

        Server server = new Server(new InetSocketAddress("localhost", 9999));
        ServletContextHandler handler = new ServletContextHandler(server, "/");
        server.setHandler(handler);
        handler.addServlet(MyHttpRpcHandler.class, "/*");
        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void startServer() {
        MyCar car = new MyCar();
        MyTiger tiger = new MyTiger();
        Dispatcher dispatcher = Dispatcher.getInstance();
        dispatcher.register(Car.class.getName(), car);
        dispatcher.register(Tiger.class.getName(), tiger);

        NioEventLoopGroup boss = new NioEventLoopGroup(20);
//        NioEventLoopGroup saf12 = new NioEventLoopGroup(3);
        NioEventLoopGroup worker = boss;
        ServerBootstrap sbs = new ServerBootstrap();
        ChannelFuture server = sbs.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        System.out.println("server accept client port:" + nioSocketChannel.remoteAddress().getPort());
                        ChannelPipeline p = nioSocketChannel.pipeline();
                        // 1.自定义的rpc
//                        p.addLast(new ServerDecode());
//                        p.addLast(new ServerRequestHandler(dispatcher));
                        // 在定义的协议的时候你关注过哪些问题：粘包拆包的问题，header+body

                        // 2.小火车，传输协议就是http 《- 可以自己学，字节码byte[]
                        // netty提供了一套编解码
                        p.addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(1024 * 512))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        FullHttpRequest request = (FullHttpRequest) msg;
                                        System.out.println(request);

                                        ByteBuf content = request.content();
                                        byte[] bytes = new byte[content.readableBytes()];
                                        content.readBytes(bytes);
                                        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bytes));
                                        MyContent myContent = (MyContent) oin.readObject();
                                        String name = myContent.getName();
                                        String methodName = myContent.getMethodName();
                                        Object car = dispatcher.get(name);
                                        Class<?> aClass = car.getClass();
                                        Object res = null;
                                        try {
                                            Method method = aClass.getMethod(methodName, myContent.getParameterTypes());
                                            res = method.invoke(car, myContent.getArgs());
                                        } catch (NoSuchMethodException e) {
                                            e.printStackTrace();
                                        } catch (IllegalAccessException e) {
                                            e.printStackTrace();
                                        } catch (InvocationTargetException e) {
                                            e.printStackTrace();
                                        }
                                        MyContent resContent = new MyContent();
                                        resContent.setRes(res);
                                        byte[] resBytes = SerUtils.serByte(resContent);
                                        DefaultHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_0,
                                                HttpResponseStatus.OK, Unpooled.copiedBuffer(resBytes));

                                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, resBytes.length);
                                        ctx.writeAndFlush(response);
                                    }
                                });
                    }
                })
                .bind(new InetSocketAddress("localhost", 9999));
        try {
            server.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void get() {

//        new Thread(() -> startServer()).start();
//        System.out.println("server started...");

        AtomicInteger num = new AtomicInteger(0);
        int size = 5;
        Thread[] threads = new Thread[size];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                Car car = MyProxy.proxyGet(Car.class);
                String arg = "BenzA45s " + num.incrementAndGet();
                String res = car.drive(arg);
                System.out.println("res : " + res);
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        Tiger tiger = proxyGet(Tiger.class);
//        tiger.eat("meat");
    }

    @Test
    public void rpcTest() {
        Car car = MyProxy.proxyGet(Car.class);
        Person armin = car.driver("ARMIN", 25);
        System.out.println(armin);
    }


}


// 解码器


// 源于 spark 源码


// 链接池


