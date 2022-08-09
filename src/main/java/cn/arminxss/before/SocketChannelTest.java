import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SocketChannelTest {

    public static void main(String[] args) throws IOException {
        test();
    }

    /**
     * 通过该例子可以知道，通过ServerSocketChannel.open()方法可以获取服务器的通道，然后绑定一个地址端口号，
     * 接着accept()方法可获得一个SocketChannel通道，也就是客户端的连接通道。
     *
     * 最后配合使用Buffer进行读写即可。
     *
     * 这就是一个简单的例子，实际上上面的例子是阻塞式的。要做到非阻塞还需要使用选择器Selector
     * @throws IOException
     */
    private static void test() throws IOException {
        //获取ServerSocketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6666);
        //绑定地址，端口号
        serverSocketChannel.bind(address);
        //创建一个缓冲区
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        while (true) {
            //获取SocketChannel
            SocketChannel socketChannel = serverSocketChannel.accept();
            while (socketChannel.read(byteBuffer) != -1) {
                //打印结果
                System.out.println(new String(byteBuffer.array()));
                //打印结果
                byteBuffer.clear();
            }
        }
    }

}
