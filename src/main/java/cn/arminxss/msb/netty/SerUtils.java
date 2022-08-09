package cn.arminxss.msb.netty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SerUtils {

    static ByteArrayOutputStream out = new ByteArrayOutputStream();

    public synchronized static byte[] serByte(Object obj) {
        out.reset();
        byte[] bytes = null;
        try {
            ObjectOutputStream oout = new ObjectOutputStream(out);
            oout.writeObject(obj);
            bytes = out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

}
