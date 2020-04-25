# Java BIO

```java
import java.net.ServerSocket;
import java.net.Socket;
```

**ServerSocket** 是服务端通过 ==accpet== 监听客户端的连接请求的。

**Socket** 是客户端与服务端通信用的。另外，ServerSocket#accept 建立连接后，也产生了 Socket 对象，用于与客户端数据交互。

