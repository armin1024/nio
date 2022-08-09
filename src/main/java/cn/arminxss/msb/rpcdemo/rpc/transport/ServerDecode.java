package cn.arminxss.msb.rpcdemo.rpc.transport;

import cn.arminxss.msb.rpcdemo.rpc.protocal.MyContent;
import cn.arminxss.msb.rpcdemo.rpc.protocal.MyHeader;
import cn.arminxss.msb.rpcdemo.util.PackageMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;

public class ServerDecode extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf buf, List<Object> list) throws Exception {
//        System.out.println("server channel start:" + buf.readableBytes());
        while (buf.readableBytes() >= 111) {
            byte[] bytes = new byte[111];
            buf.getBytes(buf.readerIndex(), bytes); // getBytes 不改变 readIndex 指针位置
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(in);
            MyHeader header = (MyHeader) oin.readObject();
//            System.out.println("server get requestId:" + header.getRequestId());
//            System.out.println("server get data length: "+ header.getDataLen());

            if (buf.readableBytes() - 111 >= header.getDataLen()) {
                buf.readBytes(111); // 指针移动到 body 的开始位
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