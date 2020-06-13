package eniso.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.Set;

public class NIOGroupChatClient implements Runnable, Closeable {

    private final SocketChannel clientChannel;

    private final Selector selector;

    private boolean stop;

    public NIOGroupChatClient(String ip, int port) throws IOException {
        clientChannel = SocketChannel.open();
        clientChannel.configureBlocking(false);
        clientChannel.connect(new InetSocketAddress(ip, port));
        while (!clientChannel.finishConnect()) {
            System.out.println("正在连接服务器 ...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        selector = Selector.open();
        clientChannel.register(selector, SelectionKey.OP_READ, null);
        stop = false;
    }

    public void readMessage() throws IOException {
        if (selector.select(1000) > 0) {
            // 得到发生关注事件的 SelectionKey 集合
            final Set<SelectionKey> selectionKeys = selector.selectedKeys();
            // 遍历处理
            for (SelectionKey selectionKey : selectionKeys) {
                if (selectionKey.isReadable()) {
                    final SelectableChannel channel = selectionKey.channel();
                    if (channel instanceof SocketChannel) {
                        final Object attachment = selectionKey.attachment();
                        ByteBuffer buffer;
                        if (attachment instanceof ByteBuffer) {
                            buffer = (ByteBuffer) attachment;
                        } else {
                            // 首次进来，创建一个 buffer
                            buffer = ByteBuffer.allocate(4096);
                            selectionKey.attach(buffer);
                        }

                        buffer.clear();
                        final int read = ((SocketChannel) channel).read(buffer);
                        if (read != -1) {
                            System.out.println(new String(buffer.array(), 0, read));
                        } else {
                            // 当客户端断开后，这里也需要断开。解决一些 JDK 在客户端断开后的问题
                            selectionKey.cancel();
                            channel.close();
                        }
                    }
                }
            }
            // 清空，很关键
            selectionKeys.clear();
        }
    }

    public void sendMessage(String msg) throws IOException {
        if (clientChannel.isConnected()) {
            clientChannel.write(ByteBuffer.wrap(msg.getBytes()));
        } else {
            stop();
            try {
                System.in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        System.out.println("客户端启动完成 ...");
        while (!stop) {
            // 服务器断开连接
            if (!clientChannel.isConnected()) {
                stop();
                break;
            }
            try {
                readMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 关闭输入
        try {
            System.in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void close() throws IOException {
        stop = true;
        if (selector != null) {
            selector.close();
        }
        if (clientChannel != null) {
            clientChannel.close();
        }
        System.out.println("客户端退出完成 ...");
    }

    public static void main(String[] args) {
        // 创建客户端，如果发生异常，那么会自动调用 close 方法释放资源
        try (NIOGroupChatClient client = new NIOGroupChatClient("127.0.0.1", 7070)) {
            // 启动一个线程读取来自服务器的数据
            new Thread(client).start();
            // 扫描用户键盘输入
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                // 读取一行数据，如果用户输入 quit，则退出程序
                String msg = scanner.nextLine();
                if (msg.equals("quit")) {
                    client.stop();
                    break;
                }
                // 发送消息
                client.sendMessage(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
