import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * 分散读取，聚合写入
 */
public class FileChannelScatterReadAggregateWrite {

    public static void main(String[] args) throws IOException {
        test();
    }

    /**
     * 使用场景就是可以使用一个缓冲区数组，自动地根据需要去分配缓冲区的大小。可以减少内存消耗。网络IO也可以使用
     *
     * @throws IOException
     */
    public static void test() throws IOException {
        File file = new File("ScatterReadAggregateWrite.txt");
        FileInputStream inputStream = new FileInputStream(file);
        FileOutputStream outputStream = new FileOutputStream("ScatterReadAggregateWrite_copy.txt");
        FileChannel inputStreamChannel = inputStream.getChannel();
        FileChannel outputStreamChannel = outputStream.getChannel();
        //创建三个缓冲区，分别都是5
        ByteBuffer byteBuffer1 = ByteBuffer.allocate(5);
        ByteBuffer byteBuffer2 = ByteBuffer.allocate(5);
        ByteBuffer byteBuffer3 = ByteBuffer.allocate(5);
        //创建一个缓冲区数组
        ByteBuffer[] buffers = {byteBuffer1, byteBuffer2, byteBuffer3};
        //循环写入到buffers缓冲区数组中，分散读取
        long read;
        long sumLength = 0;
        while ((read = inputStreamChannel.read(buffers)) != -1) {
            sumLength += read;
            Arrays.stream(buffers).map(buffer ->
                    "position=" + buffer.position() + ",limit=" + buffer.limit()).
                    forEach(System.out::println);
            //切换模式
            Arrays.stream(buffers).forEach(ByteBuffer::flip);
            //聚合写入到文件输出通道
            outputStreamChannel.write(buffers);
            //清空缓冲区
            Arrays.stream(buffers).forEach(ByteBuffer::clear);
        }
        System.out.println("总长度：" + sumLength);
        outputStreamChannel.close();
        inputStreamChannel.close();
        outputStream.close();
        inputStream.close();
    }

    /*
    result:
        position=5,limit=5
        position=5,limit=5
        position=5,limit=5
        position=5,limit=5
        position=5,limit=5
        position=1,limit=5
        总长度：26

    可以看到循环了两次。第一次循环时，三个缓冲区都读取了5个字节，总共读取了15，也就是读满了。
    还剩下11个字节，于是第二次循环时，前两个缓冲区分配了5个字节，最后一个缓冲区给他分配了1个字节，刚好读完。总共就是26个字节。
     */

}
