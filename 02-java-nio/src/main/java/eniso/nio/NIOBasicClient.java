package eniso.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NIOBasicClient {

    public static void main(String[] args) {
        // 创建 SocketChannel
        try (SocketChannel socketChannel = SocketChannel.open()) {
            // 设置为非阻塞
            socketChannel.configureBlocking(false);
            // 连接服务器
            if (!socketChannel.connect(new InetSocketAddress("127.0.0.1", 7070))) {
                while (!socketChannel.finishConnect()) {
                    // 还没有连接上服务器，这里可以做其他的工作
                    Thread.sleep(1000);
                }
            }

            // 已经连接到服务器
            socketChannel.write(ByteBuffer.wrap("hello, i'm NIOBasicClient".getBytes()));
            Thread.sleep(1000);
            socketChannel.write(ByteBuffer.wrap("hello, i'm NIOBasicClient2".getBytes()));
            Thread.sleep(1000);
            socketChannel.write(ByteBuffer.wrap("hello, i'm NIOBasicClient3".getBytes()));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
