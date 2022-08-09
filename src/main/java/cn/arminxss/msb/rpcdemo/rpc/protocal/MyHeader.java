package cn.arminxss.msb.rpcdemo.rpc.protocal;

import java.io.Serializable;
import java.util.UUID;

public class MyHeader implements Serializable {
    int flag;
    long requestId;
    int dataLen;

    public static MyHeader createHeader(byte[] msg) {
        MyHeader header = new MyHeader();
        int flag = 0x14141414;
        long requestId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        header.setFlag(flag);
        header.setDataLen(msg.length);
        header.setRequestId(requestId);
        return header;
    }

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
