package eniso.bio;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class BIOClient {

    public static void main(String[] args) {
        // 客户端通过 Socket 与服务端的 ServerSocket 建立连接
        // 通过 socket.getOutputStream() 得到输出流对象
        try (Socket socket = new Socket("127.0.0.1", 7070);
             OutputStream os = socket.getOutputStream()) {
            // 向服务端发送信息
            os.write("hello, i'm BIOClient".getBytes());
            os.flush();
            System.out.println("发送完毕");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
