package cn.arminxss.msb.rpcdemo.rpc.transport;

import cn.arminxss.msb.rpcdemo.rpc.ResCallback;
import cn.arminxss.msb.rpcdemo.util.PackageMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientResponse extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        PackageMessage packMsg = (PackageMessage) msg;
        ResCallback.runCallBack(packMsg);

    }
}