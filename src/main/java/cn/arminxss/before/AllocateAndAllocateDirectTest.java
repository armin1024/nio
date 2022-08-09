import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class AllocateAndAllocateDirectTest {

    public static void main(String[] args) throws IOException {
        test();
    }

    private static void test() throws IOException {
        long startTime = System.currentTimeMillis();
        File file = new File("/Users/armin/Documents/T7/study/JVM_COURSER/200727卡尔JVM第11期/录播文件夹/Carl的JVM深入浅出训练营第四天.mp4");
        File copy = new File("/Users/armin/Documents/T7/study/JVM_COURSER/200727卡尔JVM第11期/录播文件夹/test.mp4");
        FileInputStream inputStream = new FileInputStream(file);
        FileOutputStream outputStream = new FileOutputStream(copy);
        FileChannel inputStreamChannel = inputStream.getChannel();
        FileChannel outputStreamChannel = outputStream.getChannel();
        //创建一个非直接缓冲区
        ByteBuffer byteBuffer = ByteBuffer.allocate(5 * 1024 * 1024); // 284ms
        //创建一个直接缓冲区
//        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(5 * 1024 * 1024); //137ms
        //写入到缓冲区
        while (inputStreamChannel.read(byteBuffer) != -1) {
            //切换读模式
            byteBuffer.flip();
            outputStreamChannel.write(byteBuffer);
            byteBuffer.clear();
        }
        //关闭通道
        outputStreamChannel.close();
        inputStreamChannel.close();
        outputStream.close();
        inputStream.close();
        if (copy.exists()) {
            copy.delete();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("耗时：" + (endTime - startTime));
    }

}
