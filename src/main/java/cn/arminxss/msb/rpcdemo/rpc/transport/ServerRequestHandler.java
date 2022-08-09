package cn.arminxss.msb.rpcdemo.rpc.transport;

import cn.arminxss.msb.rpcdemo.Dispatcher;
import cn.arminxss.msb.rpcdemo.rpc.protocal.MyContent;
import cn.arminxss.msb.rpcdemo.rpc.protocal.MyHeader;
import cn.arminxss.msb.rpcdemo.util.PackageMessage;
import cn.arminxss.msb.rpcdemo.util.SerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ServerRequestHandler extends ChannelInboundHandlerAdapter {

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

            // 反射获取对象，前提注册实现类
            String name = message.getContent().getName();
            String methodName = message.getContent().getMethodName();
            Object car = dispatcher.get(name);
            Class<?> aClass = car.getClass();
            Object res = null;
            try {
                Method method = aClass.getMethod(methodName, message.getContent().getParameterTypes());
                res = method.invoke(car, message.getContent().getArgs());
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
//            String res = "io thread:" + ioThreadName + " exe thread:" + exeThreadName + " from args:" + message.getContent().getArgs()[0];
//            System.out.println(res);
            content.setRes(res);
            byte[] msgContent = SerUtils.serByte(content);
            header.setRequestId(message.getHeader().getRequestId());
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