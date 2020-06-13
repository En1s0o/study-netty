# Java NIO

NIO（**N**on-blocking **IO** / **N**ew **IO**），是**同步非阻塞**的，是面向缓冲区（或者块）编程的。服务端/客户端将数据放到 Buffer（缓冲区），需要时可以在缓冲区前后移动。注意：并不是直接操作 Channel（通道）。

## Java NIO 3 个核心概念

- **Channel**

  重要的方法

  ```java
  // 将 Channel 中的数据，写入到 ByteBuffer 中
  public abstract int read(ByteBuffer dst) throws IOException;
  public long read(ByteBuffer[] dsts) throws IOException; // ScatteringByteChannel
  // 将 ByteBuffer 中的数据，写入到 Channel 中
  public abstract int write(ByteBuffer src) throws IOException;
  public long write(ByteBuffer[] srcs) throws IOException; // GatheringByteChannel
  // 把源通道的数据 拷贝到 当前通道，0 拷贝
  public abstract long transferFrom(ReadableByteChannel src, long position, long count) throws IOException;
  // 把当前通道的数据 拷贝到 目标通道，0 拷贝
  public abstract long transferTo(long position, long count, WritableByteChannel target) throws IOException;
  // 关闭，程序结束需要关闭
  public void close() throws IOException;
  ```
  
  **ServerSocketChannel** - 重要实现类（服务端）
  
  - 重要方法
  
    **用于服务端监听客户端的连接请求**
  
    ```java
    // 打开一个 ServerSocketChannel 通道
    public static ServerSocketChannel open() throws IOException;
    // 绑定地址
    public final ServerSocketChannel bind(SocketAddress local) throws IOException;
    // 设置是否阻塞模式（默认：阻塞模式）
    public final SelectableChannel configureBlocking(boolean block) throws IOException;
    // 注册到 Selector 上，并指定关注的事件
    public final SelectionKey register(Selector sel, int ops) throws ClosedChannelException;
    // 接收来自客户端的连接请求，并创建与客户端进行后续通信的 SocketChannel 对象
    public abstract SocketChannel accept() throws IOException;
    ```
  
  **SocketChannel** - 重要实现类（网络 I/O 通道，负责读写操作）
  
  - 重要方法
  
    ```java
    // 打开一个 SocketChannel 通道
    public static SocketChannel open() throws IOException;
    // 设置是否阻塞模式（默认：阻塞模式）
    public abstract SelectableChannel configureBlocking(boolean block) throws IOException;
    // 客户端连接服务端
    public abstract boolean connect(SocketAddress remote) throws IOException;
    // 当 connect 失败时，需要通过该方法完成连接操作
    public abstract boolean finishConnect() throws IOException;
    // 将通道中的数据写入 Buffer
    public abstract int read(ByteBuffer dst) throws IOException;
    // 将 Buffer 中的数据写入通道
    public abstract int write(ByteBuffer src) throws IOException;
    // 注册到 Selector 上，并指定关注的事件
    public final SelectionKey register(Selector sel, int ops) throws ClosedChannelException;
    ```



- **Buffer**

  重要的变量

  ```java
  // Invariants: mark <= position <= limit <= capacity
  private int mark = -1;
  private int position = 0;
  private int limit;
  private int capacity;
  ```

  - position 下一个读/写元素的索引
  - limit 缓冲区的极限位置，不能对极限位置之后的缓冲区进行读写操作
  - capacity 容量，在创建 Buffer 时设置，不能修改

  flip 方法

  ```java
  public final Buffer flip() {
    limit = position; // 这个很关键，如果后续访问 limit 之后的位置，将会发生异常
    position = 0;
    mark = -1;
    return this;
  }
  ```

  - 注意细节
    -  put 和 get 的类型要一致。不一致将得到错误的数据，甚至可能抛异常
  -  如果在使用 Buffer 的过程中，不太顺利，更多细节最好是查看源码。
  



- **Selector**

  **SelectionKey** - 重要的相关类

  - 重要方法

    ```java
    public abstract SelectableChannel channel(); // 得到 Channel
    public abstract Selector selector(); // 得到 Selector
    public abstract void cancel(); // 取消，在连接断开之后，为了兼容不同平台，应该执行 cannel
    public abstract int interestOps(); // 获取关注的事件
    public abstract SelectionKey interestOps(int ops); // 设置/修改 关注的事件
    public final boolean isReadable(); // 是否可读
    public final boolean isWritable(); // 是否可写
    public final boolean isConnectable(); // 是否可以 connect
    public final boolean isAcceptable(); // 是否可以 accept
    public final Object attach(Object ob); // 可以把对象 attach 进去
    public final Object attachment(); // 获取到 attach 对象
    ```

    



## 服务端

通过 ServerSocketChannel 启动监听，当有客户端连接时，生成 SocketChannel 并注册到 Selector 上，一个 Selector 可以注册多个 SocketChannel。

SocketCahnnel 注册到 Selector 之后，返回一个 SelectionKey，会和该 Selector 关联。

Selector 通过 select 方法进行监听，返回有事件发生的通道总数。我们可以进一步得到 SelectionKey，通过 SelectionKey#channel() 便可以得到 SocketChannel，进行后续通信。





## mmap 和 sendFile

- mmap 适合小数据量读写，sendFile 适合大文件传输
- mmap 需要 4 次上下文切换，3 次数据拷贝；sendFile 需要 3 次上下文切换，最少 2 次数据拷贝
- sendFile 可以利用 DMA 方式，减少 CPU 拷贝，mmap 则不能（必须从内核拷贝到 Socket 缓冲区）

