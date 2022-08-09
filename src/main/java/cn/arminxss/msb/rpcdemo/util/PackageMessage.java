package cn.arminxss.msb.rpcdemo.util;

import cn.arminxss.msb.rpcdemo.rpc.protocal.MyContent;
import cn.arminxss.msb.rpcdemo.rpc.protocal.MyHeader;

public class PackageMessage {

    private MyHeader header;
    private MyContent content;

    public PackageMessage(MyHeader header, MyContent content) {
        this.header = header;
        this.content = content;
    }

    public MyHeader getHeader() {
        return header;
    }

    public void setHeader(MyHeader header) {
        this.header = header;
    }

    public MyContent getContent() {
        return content;
    }

    public void setContent(MyContent content) {
        this.content = content;
    }
}