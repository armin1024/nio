package cn.arminxss.msb.rpcdemo.rpc.transport;

import cn.arminxss.msb.rpcdemo.rpc.ResCallback;
import cn.arminxss.msb.rpcdemo.rpc.protocal.MyContent;
import cn.arminxss.msb.rpcdemo.rpc.protocal.MyHeader;
import cn.arminxss.msb.rpcdemo.util.SerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ClientFactory {
    // 一个 consumer 可以链接很多 provider ，每个 provider 都有自己的pool K,V
    int poolSize = 1; // 自定义连接数
    Random rand = new Random();
    final static ClientFactory factory;

    static {
        factory = new ClientFactory();
    }
    private ClientFactory() {

    }
    public static ClientFactory getFactory() {
        return factory;
    }

    ConcurrentHashMap<InetSocketAddress, ClientPool> outboxes = new ConcurrentHashMap<>();

    public static CompletableFuture<Object> transport(MyContent content) {
        CompletableFuture<Object> res = new CompletableFuture<>();

//        String type = "rpc";
        String type = "http";

        if (type.equals("rpc")) {
            byte[] msgBody = SerUtils.serByte(content);
            // 2.requestId + message，本地缓存
            MyHeader header = MyHeader.createHeader(msgBody);
            byte[] msgHeader = SerUtils.serByte(header);
            System.out.println("header length:" + msgHeader.length);
            // 3.链接池 ：：取得链接
            ClientFactory factory = ClientFactory.getFactory();
            NioSocketChannel client = factory.getClient(new InetSocketAddress("localhost", 9999));
            ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(msgHeader.length + msgBody.length);
            ResCallback.addCallBack(header.getRequestId(), res);
            byteBuf.writeBytes(msgHeader);
            byteBuf.writeBytes(msgBody);
            client.writeAndFlush(byteBuf);
        } else {
            //使用http协议为载体
            //1，用URL 现成的工具（包含了http的编解码，发送，socket，连接）
            urlTransport(content, res);

            //2，自己操心：on netty  （io 框架）+ 已经提供的http相关的编解码
//            nettyTransport(content, res);
        }
        return res;
    }

    private static void nettyTransport(MyContent content, CompletableFuture<Object> res) {
        //在这个执行之前  我们的server端 provider端已经开发完了，已经是on netty的http server了
        //现在做的事consumer端的代码修改，改成 on netty的http client
        //刚才一切都顺利，关注未来的问题。。。。。。

        //每个请求对应一个连接
        //通过netty建立io 建立连接
        //TODO  :  [server使用jetty]改成 多个http的request 复用一个 netty client，而且 并发发送请求
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bs = new Bootstrap();
        Bootstrap client = bs.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        ChannelPipeline p = nioSocketChannel.pipeline();
                        p.addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(1024 * 512))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        // 接收   预埋的回调，根据netty对socket io 事件的响应
                                        // 客户端的msg是啥：完整的http-response
                                        FullHttpResponse response = (FullHttpResponse) msg;
                                        System.out.println(response);

                                        ByteBuf byteContent = response.content();
                                        byte[] data = new byte[byteContent.readableBytes()];
                                        byteContent.readBytes(data);

                                        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(data));
                                        MyContent myContent = (MyContent) oin.readObject();

                                        res.complete(myContent.getRes());
                                    }
                                });
                    }
                });
        try {
            // 链接
            ChannelFuture syncFuture = client.connect("localhost", 9999).sync();
            // 发送
            Channel clientChannel = syncFuture.channel();
            byte[] data = SerUtils.serByte(content);
            DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0,
                    HttpMethod.POST, "/", Unpooled.copiedBuffer(data));

            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, data.length);

            clientChannel.writeAndFlush(request).sync(); // 作为client 向server端发送：http  request
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void urlTransport(MyContent content, CompletableFuture<Object> res) {
        // 这种方式是每请求占用一个连接的方式，因为使用的是http协议
        Object obj = null;
        try {
            URL url = new URL("http://localhost:9999");
            // 建立链接
            HttpURLConnection hc = (HttpURLConnection) url.openConnection();
            hc.setRequestMethod("POST");
            hc.setDoOutput(true);
            hc.setDoInput(true);

            // 发送
            OutputStream out = hc.getOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(out);
            oout.writeObject(content); // 不再此处发送数据

            // 接收
            if (hc.getResponseCode() == 200) { // 此处发送请求数据，阻塞的
                InputStream in = hc.getInputStream();
                ObjectInputStream oin = new ObjectInputStream(in);
                MyContent myContent = (MyContent) oin.readObject();
                obj = myContent.getRes();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        // 返回
        res.complete(obj);
    }

    public NioSocketChannel getClient(InetSocketAddress address) {
        // TODO 并发情况下一定要谨慎
        ClientPool clientPool = outboxes.get(address);
        if (clientPool == null) {
            synchronized (outboxes) {
                if (clientPool == null) {
                    outboxes.putIfAbsent(address, new ClientPool(poolSize));
                    clientPool = outboxes.get(address);
                }
            }
        }
        int i = rand.nextInt(poolSize);
        if (clientPool.clients[i] != null && clientPool.clients[i].isActive()) {
            return clientPool.clients[i];
        } else {
            synchronized (clientPool.lock[i]) {
                if (clientPool.clients[i] == null || !clientPool.clients[i].isActive()) {
                    clientPool.clients[i] = create(address);
                }
            }
        }
        return clientPool.clients[i];
    }

    private NioSocketChannel create(InetSocketAddress address) {
        // 基于 netty 客户端创建方式
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bs = new Bootstrap();
        ChannelFuture connect = bs.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        ChannelPipeline p = nioSocketChannel.pipeline();
                        p.addLast(new ServerDecode());
                        p.addLast(new ClientResponse());
                    }
                })
                .connect(address);
        try {
            return (NioSocketChannel) connect.sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}