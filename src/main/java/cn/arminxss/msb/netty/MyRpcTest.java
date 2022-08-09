package cn.arminxss.msb.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class MyRpcTest {

    @Test
    public void startServer() {
        MyCar car = new MyCar();
        MyTiger tiger = new MyTiger();
        Dispatcher dispatcher = new Dispatcher();
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
                        p.addLast(new ServerDecoder());
                        p.addLast(new ServerRequestHandler(dispatcher));
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

        new Thread(() -> startServer()).start();
        System.out.println("server started...");

        AtomicInteger num = new AtomicInteger(0);
        int size = 5;
        Thread[] threads = new Thread[size];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                Car car = proxyGet(Car.class);
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

    public static <T>T proxyGet(Class<T> interfaceInfo) {
        ClassLoader loader = interfaceInfo.getClassLoader();
        Class<?>[] methodInfo = {interfaceInfo};
        return (T) Proxy.newProxyInstance(loader, methodInfo, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 设计consumer对于provider的调用过程
                String name = interfaceInfo.getName(); // 可根据name（接口名称）获取到实现该接口类型的实现类
                String methodName = method.getName();
                Class<?>[] parameterTypes = method.getParameterTypes();
                MyContent content = new MyContent();
                content.setName(name);
                content.setMethodName(methodName);
                content.setParameterTypes(parameterTypes);
                content.setArgs(args);
                // 1.调用服务，方法，参数 ==》 message
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bout);
                out.writeObject(content);
                byte[] msgBody = bout.toByteArray();
                // 2.requestId + message，本地缓存
                MyHeader header = createHeader(msgBody);
                bout.reset();
                out = new ObjectOutputStream(bout);
                out.writeObject(header);
                // todo 解决数据的 decoder 问题
                byte[] msgHeader = bout.toByteArray();
                System.out.println("header length:" + msgHeader.length);
                // 3.链接池 ：：取得链接
                ClientFactory factory = ClientFactory.getFactory();
                NioSocketChannel client = factory.getClient(new InetSocketAddress("localhost", 9999));
                // 获取链接过程中：开始-创建，过程-直接获取

                // 4.发送 --> 走IO out --> 走netty（event驱动）
                ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(msgHeader.length + msgBody.length);
//                CountDownLatch countDownLatch = new CountDownLatch(1);
                // 定义callBack，停止阻塞
//                ResCallback.addCallBack(header.getRequestId(), () -> countDownLatch.countDown());
                CompletableFuture<String> res = new CompletableFuture<>();
                ResCallback.addCallBack(header.getRequestId(), res);
                byteBuf.writeBytes(msgHeader);
                byteBuf.writeBytes(msgBody);
                ChannelFuture channelFuture = client.writeAndFlush(byteBuf);
                channelFuture.sync(); // io是双向的，看似有个sync，他仅代表out
//                countDownLatch.await();
                // 5.如果从IO，未来回来时，如何将代码执行到这里（睡眠/回调，如何让线程停下来？还能让它继续。。）

                return res.get(); // 阻塞的
            }
        });
    }

    private static MyHeader createHeader(byte[] msg) {
        MyHeader header = new MyHeader();
        int flag = 0x14141414;
        long requestId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        header.setFlag(flag);
        header.setDataLen(msg.length);
        header.setRequestId(requestId);
        return header;
    }

}

class PackageMessage {

    MyHeader header;
    MyContent content;

    public PackageMessage(MyHeader header, MyContent content) {
        this.header = header;
        this.content = content;
    }
}

// 解码器
class ServerDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf buf, List<Object> list) throws Exception {
//        System.out.println("server channel start:" + buf.readableBytes());
        while (buf.readableBytes() >= 96) {
            byte[] bytes = new byte[96];
            buf.getBytes(buf.readerIndex(), bytes); // getBytes 不改变 readIndex 指针位置
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(in);
            MyHeader header = (MyHeader) oin.readObject();
//            System.out.println("server get requestId:" + header.getRequestId());
//            System.out.println("server get data length: "+ header.getDataLen());

            if (buf.readableBytes() - 96 >= header.getDataLen()) {
                buf.readBytes(96); // 指针移动到 body 的开始位
                byte[] data = new byte[header.getDataLen()];
                buf.readBytes(data);
                ByteArrayInputStream din = new ByteArrayInputStream(data);
                ObjectInputStream doin = new ObjectInputStream(din);
                if (header.getFlag() == 0x14141414) {
                    MyContent content = (MyContent) doin.readObject();
//                System.out.println("sever get class name:" + content.getName());
                    list.add(new PackageMessage(header, content));
                } else if (header.getFlag() == 0x14141424) {
                    MyContent content = (MyContent) doin.readObject();
                    list.add(new PackageMessage(header, content));
                }
            } else {
                break;
            }
        }
    }
}

class ServerRequestHandler extends ChannelInboundHandlerAdapter {

    Dispatcher dispatcher;

    public ServerRequestHandler(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    // provider
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        PackageMessage message = (PackageMessage) msg;
//        System.out.println("server handler:" + message.content.getArgs()[0]);

        String ioThreadName = Thread.currentThread().getName(); // ClientPool初始链接数决定 io thread个数
        // 此处有两种执行方式：1.自己开辟一个新的线程进行执行；2.使用netty自带EventLoopGroup进行执行（此处选用该方式）
        ctx.executor().execute(() -> { // 在当前线程继续执行处理，io thread = exe thread
//        ctx.executor().parent().next().execute(() -> { // 新开线程继续执行以下业务（依赖EventLoopGroup个数）

            String name = message.content.getName();
            String methodName = message.content.getMethodName();
            Object car = dispatcher.get(name);
            Class<?> aClass = car.getClass();
            Object res = null;
            try {
                Method method = aClass.getMethod(methodName, message.content.getParameterTypes());
                res = method.invoke(car, message.content.getArgs());
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            String exeThreadName = Thread.currentThread().getName(); // exe thread 个数取决于EventLoopGroup个数
            MyHeader header = new MyHeader();
            MyContent content = new MyContent();
//            String res = "io thread:" + ioThreadName + " exe thread:" + exeThreadName + " from args:" + message.content.getArgs()[0];
//            System.out.println(res);
            content.setRes((String) res);
            byte[] msgContent = SerUtils.serByte(content);
            header.setRequestId(message.header.getRequestId());
            header.setFlag(0x14141424);
            header.setDataLen(msgContent.length);
            byte[] msgHeader = SerUtils.serByte(header);
            ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(msgHeader.length + msgContent.length);
            byteBuf.writeBytes(msgHeader);
            byteBuf.writeBytes(msgContent);
            ctx.writeAndFlush(byteBuf);
        });

    }
}

class ResCallback {
    static ConcurrentHashMap<Long, CompletableFuture<String>> mapping = new ConcurrentHashMap<>();

    public static void addCallBack(long requestId, CompletableFuture<String> completableFuture) {
        mapping.putIfAbsent(requestId, completableFuture);
    }

    public static void runCallBack(PackageMessage message) {
        CompletableFuture<String> res = mapping.get(message.header.getRequestId());
        res.complete(message.content.getRes());
        remove(message.header.getRequestId());
    }

    private static void remove(long requestId) {
        mapping.remove(requestId);
    }

}

class ClientResponse extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        ByteBuf buf = (ByteBuf) msg;
//        if (buf.readableBytes() >= 96) {
//            byte[] bytes = new byte[96];
//            buf.readBytes(bytes);
//            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
//            ObjectInputStream oin = new ObjectInputStream(in);
//            MyHeader header = (MyHeader) oin.readObject();
//            System.out.println("client response @ id:" + header.getRequestId());
//            // callBack
//            ResCallback.runCallBack(header.getRequestId());
//        }
        PackageMessage packMsg = (PackageMessage) msg;
        ResCallback.runCallBack(packMsg);

    }
}

// 源于 spark 源码
class ClientFactory {
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

    public synchronized NioSocketChannel getClient(InetSocketAddress address) {
        ClientPool clientPool = outboxes.get(address);
        if (clientPool == null) {
            outboxes.putIfAbsent(address, new ClientPool(poolSize));
            clientPool = outboxes.get(address);
        }
        int i = rand.nextInt(poolSize);
        if (clientPool.clients[i] != null && clientPool.clients[i].isActive()) {
            return clientPool.clients[i];
        }
        synchronized (clientPool.lock[i]) {
            return clientPool.clients[i] = create(address);
        }
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
                        p.addLast(new ServerDecoder());
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

// 链接池
class ClientPool {
    NioSocketChannel[] clients;
    Object[] lock;

    ClientPool(int size) {
        clients = new NioSocketChannel[size]; // init 链接是空的
        lock = new Object[size];
        for (int i = 0; i < lock.length; i++) {
            lock[i] = new Object();
        }
    }

}

class MyHeader implements Serializable {
    int flag;
    long requestId;
    int dataLen;

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public int getDataLen() {
        return dataLen;
    }

    public void setDataLen(int dataLen) {
        this.dataLen = dataLen;
    }
}

class MyContent implements Serializable {
    String name;
    String methodName;
    Class<?>[] parameterTypes;
    Object[] args;
    String res;

    public String getRes() {
        return res;
    }

    public void setRes(String res) {
        this.res = res;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }
}

class Dispatcher {
    static ConcurrentHashMap<String, Object> invokeMap = new ConcurrentHashMap<>();

    public void register(String key, Object val) {
        invokeMap.put(key, val);
    }

    public Object get(String key) {
        return invokeMap.get(key);
    }
}

class MyCar implements Car {

    @Override
    public String drive(String msg) {
        System.out.println("[Car]server get client arg:" + msg);
        return "server res:" + msg;
    }
}

class MyTiger implements Tiger {
    @Override
    public void eat(String food) {
        System.out.println("[Tiger]server get client arg:" + food);
    }
}


interface Tiger {

    void eat(String food);

}

interface Car {

    String drive(String msg);

}