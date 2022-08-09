import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelTest {

    public static void main(String[] args) throws IOException {
        test();
    }

    public static void test() throws IOException {
        //获取文件输入流
        File file = new File("channel.txt");
        FileInputStream inputStream = new FileInputStream(file);
        //从文件输入流获取通道
        FileOutputStream outputStream = new FileOutputStream("channel_copy.txt");
        //获取文件输出流
        FileChannel inputStreamChannel = inputStream.getChannel();
        //从文件输出流获取通道
        FileChannel outputStreamChannel = outputStream.getChannel();
        //创建一个byteBuffer，小文件所以就直接一次读取，不分多次循环了
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) file.length());
        //把输入流通道的数据读取到缓冲区
        inputStreamChannel.read(byteBuffer);
        //切换成读模式
        byteBuffer.flip();
        //把数据从缓冲区写入到输出流通道
        outputStreamChannel.write(byteBuffer);
        //关闭通道
        outputStreamChannel.close();
        inputStreamChannel.close();
        outputStream.close();
        inputStream.close();
    }

}
