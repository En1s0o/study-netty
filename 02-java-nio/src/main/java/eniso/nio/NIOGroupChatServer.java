package eniso.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Scanner;
import java.util.Set;

public class NIOGroupChatServer implements Runnable, Closeable {

    private final ServerSocketChannel serverChannel;

    private final Selector selector;

    private boolean stop;

    public NIOGroupChatServer(int port) throws IOException {
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();
        // 设置为非阻塞
        serverChannel.configureBlocking(false);
        // 绑定端口
        serverChannel.bind(new InetSocketAddress(port));
        // 注册到 Selector 上，监听 OP_ACCEPT 连接请求事件
        serverChannel.register(selector, SelectionKey.OP_ACCEPT, null);
        stop = false;
    }

    public void stop() {
        stop = true;
    }

    private void handler(SelectionKey selectionKey) throws IOException {
        if (selectionKey == null) {
            return;
        }
        final SelectableChannel channel = selectionKey.channel();
        if (selectionKey.isAcceptable()) {
            // final SocketChannel socketChannel = serverChannel.accept();
            if (channel instanceof ServerSocketChannel) {
                final SocketChannel socketChannel = ((ServerSocketChannel) channel).accept();
                socketChannel.configureBlocking(false);
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                // attachment 可以传递任何数据，这里只是简单的传递了 buffer
                socketChannel.register(selector, SelectionKey.OP_READ, buffer);
                String msg = "新用户 '" + socketChannel.getRemoteAddress().toString().substring(1) + "' 上线";
                System.err.println(msg);
                dispatchToOthers(socketChannel, msg);
            }
        } else if (selectionKey.isReadable()) {
            // 暂时只关注 SocketChannel
            if (channel instanceof SocketChannel) {
                final Object attachment = selectionKey.attachment();
                ByteBuffer buffer;
                if (attachment instanceof ByteBuffer) {
                    // 当交互比较频繁时，使用 attachment，相比每次都创建一个 buffer 更合理
                    buffer = (ByteBuffer) attachment;
                } else {
                    buffer = ByteBuffer.allocate(4096);
                    // 其实也不一定一开始就传递 buffer 到 attachment，也可以这么传递
                    selectionKey.attach(buffer);
                }

                String username = ((SocketChannel) channel).getRemoteAddress().toString().substring(1);
                try {
                    buffer.clear();
                    final int read = ((SocketChannel) channel).read(buffer);
                    if (read != -1) {
                        String msg = "用户 '" + username + "' 说 : " + new String(buffer.array(), 0, read);
                        System.out.println(msg);
                        dispatchToOthers((SocketChannel) channel, msg);
                    } else {
                        throw new IOException("读取长度异常 -1");
                    }
                } catch (IOException e) {
                    // 当客户端断开后，这里也需要断开。解决一些 JDK 在客户端断开后的问题
                    selectionKey.cancel();
                    channel.close();
                    String msg = "用户 '" + username + "' 下线";
                    System.err.println(msg);
                    dispatchToOthers((SocketChannel) channel, msg);
                }
            }
        }
    }

    private void dispatchToOthers(SocketChannel excluded, String msg) {
        final Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            final SelectableChannel channel = key.channel();
            if (channel != excluded && channel instanceof SocketChannel) {
                try {
                    ((SocketChannel) channel).write(ByteBuffer.wrap(msg.getBytes()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("服务器启动完成 ...");
            while (!stop) {
                if (selector.select(5000) > 0) {
                    final Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    for (SelectionKey selectionKey : selectionKeys) {
                        try {
                            handler(selectionKey);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    selectionKeys.clear();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            stop = true;
        }
    }

    @Override
    public void close() throws IOException {
        stop = true;
        if (selector != null) {
            selector.close();
        }
        if (serverChannel != null) {
            serverChannel.close();
        }
        System.out.println("服务器退出完成 ...");
    }

    public static void main(String[] args) {
        try (NIOGroupChatServer server = new NIOGroupChatServer(7070)) {
            new Thread(server).start();
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                final String msg = scanner.nextLine();
                if (msg.equals("quit")) {
                    server.stop();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
