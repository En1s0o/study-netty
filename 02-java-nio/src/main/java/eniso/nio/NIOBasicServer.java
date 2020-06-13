package eniso.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Set;

public class NIOBasicServer {

    public static void main(String[] args) {
        // 创建 ServerSocketChannel 和 Selector
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
             Selector selector = Selector.open()) {
            // 绑定端口
            serverSocketChannel.bind(new InetSocketAddress(7070));
            // 设置为非阻塞
            serverSocketChannel.configureBlocking(false);
            // 把 ServerSocketChannel 注册到 Selector 上，ServerSocketChannel 只处理连接请求 OP_ACCEPT
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, null);
            System.out.println("NIOBasicServer started");

            while (true) {
                // 等待客户端连接，注意，此时可能有 bug，导致空轮询
                if (selector.select(5000) == 0) {
                    // 没有 ServerSocket 关心的事件 OP_ACCEPT 发生
                    continue;
                }

                // #selectedKeys() 返回的是有事件发生的 SelectionKey 的集合
                // #keys() 返回所有注册在 Selector 上的 SelectionKey 的集合
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    SelectableChannel channel = selectionKey.channel();
                    if (selectionKey.isAcceptable()) {
                        // OP_ACCEPT 事件，这里调用 accept 将很快返回
                        // final SocketChannel socketChannel = serverSocketChannel.accept();
                        if (channel instanceof ServerSocketChannel) {
                            final SocketChannel socketChannel = ((ServerSocketChannel) channel).accept();
                            // 设置为非阻塞
                            socketChannel.configureBlocking(false);
                            // 与客户端通信，我们这里只关注 OP_READ 事件，这里也可以通过 ByteBuffer.allocate(4096) 指定 Buffer
                            socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(4096));
                        } else {
                            System.out.println("收到 OP_ACCEPT " + channel.getClass());
                        }
                    } else if (selectionKey.isReadable()) {
                        // OP_READ 事件
                        if (channel instanceof SocketChannel) {
                            Object attachment = selectionKey.attachment();
                            ByteBuffer buffer;
                            if (attachment instanceof ByteBuffer) {
                                // 这便是上面注册时关联的 Buffer
                                buffer = (ByteBuffer) attachment;
                            } else {
                                // 上面只是演示注册时关联 Buffer，如果没有关联 Buffer，需要自己手动创建一个 Buffer
                                buffer = ByteBuffer.allocate(4096);
                                // 为了减少创建次数，这里可以 attach 到 selectionKey 上
                                selectionKey.attach(buffer);
                            }
                            // 清空 buffer
                            buffer.clear();
                            // 将通道中的数据，写入到 buffer
                            final int read = ((SocketChannel) channel).read(buffer);
                            if (read != -1) {
                                System.out.println("接收到客户端的数据：" + new String(buffer.array(), 0, read));
                            } else {
                                // 当客户端断开后，这里也需要断开。解决一些 JDK 在客户端断开后的问题
                                selectionKey.cancel();
                                channel.close();
                            }
                        }
                    }
                }
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
