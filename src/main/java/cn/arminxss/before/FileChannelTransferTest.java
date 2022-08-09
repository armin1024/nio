import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelTransferTest {

    public static void main(String[] args) throws IOException {
//        transferToTest();
        transferFrom();
    }

    /**
     * transferTo()：把源通道的数据传输到目的通道中。
     *
     * @throws IOException
     */
    public static void transferToTest() throws IOException {
        //获取文件输入流
        File file = new File("channel.txt");
        FileInputStream inputStream = new FileInputStream(file);
        //获取文件输出流
        FileOutputStream outputStream = new FileOutputStream("channel_transfer_to.txt");
        //从文件输入流获取通道
        FileChannel inputStreamChannel = inputStream.getChannel();
        //从文件输出流获取通道
        FileChannel outputStreamChannel = outputStream.getChannel();
        //创建一个byteBuffer，小文件所以就直接一次读取，不分多次循环了
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) file.length());
        //把输入流通道的数据读取到输出流的通道
        inputStreamChannel.transferTo(0, byteBuffer.limit(), outputStreamChannel);
        //关闭通道
        outputStreamChannel.close();
        inputStreamChannel.close();
        outputStream.close();
        inputStream.close();
    }

    /**
     * transferFrom()：把来自源通道的数据传输到目的通道。
     */
    public static void transferFrom() throws IOException {
        //获取文件输入流
        File file = new File("channel.txt");
        FileInputStream inputStream = new FileInputStream(file);
        //获取文件输出流
        FileOutputStream outputStream = new FileOutputStream("channel_transfer_from.txt");
        //从文件输入流获取通道
        FileChannel inputStreamChannel = inputStream.getChannel();
        //从文件输出流获取通道
        FileChannel outputStreamChannel = outputStream.getChannel();
        //创建一个byteBuffer，小文件所以就直接一次读取，不分多次循环了
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) file.length());
        //把输入流通道的数据读取到输出流的通道
        outputStreamChannel.transferFrom(inputStreamChannel, 0, byteBuffer.limit());
        //关闭通道
        outputStreamChannel.close();
        inputStreamChannel.close();
        outputStream.close();
        inputStream.close();
    }

}
