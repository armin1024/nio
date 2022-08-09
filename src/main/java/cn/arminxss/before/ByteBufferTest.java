import java.nio.ByteBuffer;

public class ByteBufferTest {


    public static void main(String[] args) {
        test();
    }

    /**
     * HeapByteBuffer所创建的字节缓冲区就是在JVM堆中的，即JVM内部所维护的字节数组。而DirectByteBuffer是直接操作操作系统本地代码创建的内存缓冲数组。
     */
    private void tag() {
        // 创建堆内内存块HeapByteBuffer
        ByteBuffer byteBuffer1 = ByteBuffer.allocate(1024);
        /*
            HeapByteBuffer的使用场景：
            除了以上的场景外，其他情况还是建议使用HeapByteBuffer，没有达到一定的量级，实际上使用DirectByteBuffer是体现不出优势的。
         */
        String msg = "静坐常思已过！";
        // 包装一个byte[]数组获得一个Buffer，实际类型是HeapByteBuffer
        ByteBuffer byteBuffer2 = ByteBuffer.wrap(msg.getBytes());
        // 创建堆外内存块DirectByteBuffer
        ByteBuffer byteBuffer3 = ByteBuffer.allocateDirect(1024);
        /*
            DirectByteBuffer的使用场景：
            java程序与本地磁盘、socket传输数据
            大文件对象，可以使用。不会受到堆内存大小的限制。
            不需要频繁创建，生命周期较长的情况，能重复使用的情况。
         */
    }

    private static void test() {
        String msg = "静坐常思已过！";
        //创建一个固定大小的buffer(返回的是HeapByteBuffer)
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byte[] bytes = msg.getBytes();
        //写入数据到Buffer中
        byteBuffer.put(bytes);
        //切换成读模式，关键一步
        byteBuffer.flip();
        //创建一个临时数组，用于存储获取到的数据
        byte[] tempBytes = new byte[bytes.length];
        int i = 0;
        //如果还有数据，就循环。循环判断条件
        while (byteBuffer.hasRemaining()) {
            //获取byteBuffer中的数据
            byte b = byteBuffer.get();
            //放到临时数组中
            tempBytes[i] = b;
            i++;
        }
        System.out.println(new String(tempBytes));
    }

}
