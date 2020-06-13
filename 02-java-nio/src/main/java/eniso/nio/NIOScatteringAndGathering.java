package eniso.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class NIOScatteringAndGathering {

    private static void server() {
        // 创建服务端，绑定 7070 端口
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            InetSocketAddress address = new InetSocketAddress(7070);
            serverSocketChannel.bind(address);
            // 使用下面的方式也是可以的
            // serverSocketChannel.socket().bind(address);

            // 创建一个 ByteBuffer 数组
            ByteBuffer[] buffers = {ByteBuffer.allocate(5), ByteBuffer.allocate(3)};
            // 计算 buffers 的总容量
            final int maxCapacity = Arrays.stream(buffers)
                    .map(Buffer::capacity)
                    .reduce(Integer::sum)
                    .orElse(0);

            // 监听客户端
            final SocketChannel socketChannel = serverSocketChannel.accept();

            long totalRead = 0;
            long totalWrite = 0;
            while (true) { // 循环读取
                // 清除 buffers
                Arrays.stream(buffers).forEach(ByteBuffer::clear);

                // 读取数据
                final long read = socketChannel.read(buffers);
                if (read == -1) {
                    // 关闭输出
                    socketChannel.shutdownOutput();
                    break;
                }
                totalRead += read;
                System.out.println("累计读取到 " + totalRead + " 字节，当前读取到 " + read + " 字节，最大容量 " + maxCapacity + " 字节");
                Arrays.stream(buffers)
                        .map(b -> "position=" + b.position() + ", limit=" + b.limit())
                        .forEach(System.out::println);

                // 回显数据
                // 对所有的 buffer 执行读写切换
                Arrays.stream(buffers).forEach(Buffer::flip);
                // 计算 buffers 总大小
                int maxLimit = Arrays.stream(buffers)
                        .map(Buffer::limit)
                        .reduce(Integer::sum)
                        .orElse(0);
                final long write = socketChannel.write(buffers);
                totalWrite += write;
                System.out.println("累计写入了 " + totalWrite + " 字节，当前写入了 " + write + " 字节，当前总 limit " + maxLimit + " 字节");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void client() {
        try (SocketChannel socketChannel = SocketChannel.open()) {
            if (socketChannel.connect(new InetSocketAddress("127.0.0.1", 7070))) {
                socketChannel.write(ByteBuffer.wrap("hello".getBytes()));
                socketChannel.write(ByteBuffer.wrap("12345".getBytes()));
                // 发送完毕，关闭输出
                socketChannel.shutdownOutput();

                // 接收数据
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                while (true) {
                    buffer.clear();
                    final int read = socketChannel.read(buffer);
                    if (read == -1) {
                        break;
                    }
                    System.out.println("回显数据：" + new String(buffer.array(), 0, read));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        // Scattering 将数据写入到 Buffer 时候，可以使用 Buffer 数组，依次写入
        // Gathering 从 Buffer 读取数据时，可以使用 Buffer 数组，依次读出
        new Thread(NIOScatteringAndGathering::server).start();
        Thread.sleep(1000);
        client();
        Thread.sleep(1000);

//      累计读取到 8 字节，当前读取到 8 字节，最大容量 8 字节
//      position=5, limit=5
//      position=3, limit=3
//      TODO: 这里表示 buffers 中的两个 buffer 都被使用了，说明 Scattering 生效了
//      累计写入了 8 字节，当前写入了 8 字节，当前总 limit 8 字节
//      累计读取到 10 字节，当前读取到 2 字节，最大容量 8 字节
//      position=2, limit=5
//      position=0, limit=3
//      TODO: 这里表示只是使用 buffers 中的第一个 buffer，第二个 buffer 并没有使用到
//      回显数据：hello123
//      TODO: 客户端能一次拿到 hello123，说明 Gathering 生效了
//      累计写入了 10 字节，当前写入了 2 字节，当前总 limit 2 字节
//      回显数据：45
    }

}
