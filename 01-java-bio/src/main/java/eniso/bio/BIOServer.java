package eniso.bio;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BIOServer {

    public static void main(String[] args) {
        // 1. 创建一个线程池
        Executor executor = Executors.newCachedThreadPool();
        // 2. 创建 ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(7070)) {
            System.out.println("BIO Server started");
            while (true) {
                // 监听：等待客户端连接，accept 阻塞，直到有客户端连接
                // 连接建立后，可以通过 socket 对象与客户端进行通信
                final Socket socket = serverSocket.accept();
                System.out.println("BIO Server accept a client ...");
                // 使用线程池中的线程与客户端通信
                executor.execute(new MyHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class MyHandler implements Runnable {
        final Socket socket;

        MyHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            // 得到输入流，接收客户端发送的数据
            try (InputStream is = socket.getInputStream()) {
                StringBuilder sb = new StringBuilder();
                byte[] buffer = new byte[4096];
                // 循环读取客户端的数据
                while (true) {
                    int read = is.read(buffer);
                    // -1 表示读取完毕
                    if (read == -1) {
                        break;
                    }
                    sb.append(new String(buffer, 0, read));
                }
                System.out.println("收到客户端信息：" + sb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
