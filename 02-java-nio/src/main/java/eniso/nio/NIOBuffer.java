package eniso.nio;

import java.nio.*;

public class NIOBuffer {

    private static void basicTest() {
        // 创建一个大小为 7 的 Integer Buffer
        // 后续不可以修改 Buffer 的大小（capacity），可以查看源码 final int[] hb;
        IntBuffer intBuffer = IntBuffer.allocate(7);

        // 存放数据
        for (int i = 0; i < intBuffer.capacity() - 2; i++) {
            // 每次 put() / get()，如果不指定 index，都会改变 position
            System.out.println("当前 position = " + intBuffer.position());
            intBuffer.put(i * 2);
        }

        // 读写切换
        intBuffer.flip();
        // 读取数据
        while (intBuffer.hasRemaining()) {
            // 内部维护了 position 索引，执行一个 get，position 加 1
            System.out.print(intBuffer.get() + " ");
        }
        /*
        for (int i = 0; i < intBuffer.limit(); i++) {
            // 如果超过了 limit，就会抛出 java.nio.BufferUnderflowException
            System.out.print(intBuffer.get() + " ");
        }
        */
        System.out.println();

        // 不推荐：虽然可以通过遍历打印出 arr 值，但是后面 2 个并不是有效数据
        int[] hb = intBuffer.array();
        // 这里也能修改原始数据，危险操作
        hb[6] = 100;
        for (int h : hb) {
            System.out.print(h + " ");
        }
        System.out.println();

        // 错误：使用 flip，尝试写入超过 limit 长度（但是在 capacity 内）的数据
        try {
            // 读写切换
            intBuffer.flip();
            // 如果手动修改 limit 的值，那么下面代码就不会发生异常
            // intBuffer.limit(intBuffer.capacity());
            // 存放数据
            for (int i = 0; i < intBuffer.capacity(); i++) {
                intBuffer.put(i * 3);
            }
        } catch (BufferOverflowException e) {
            // 由于第一次 flip 的时候，limit 为 7 - 2 = 5，所以这里使用 capacity，自然就溢出了
            // 如果需要重新赋值，应该使用 clear，而不是 flip
            System.out.println("即使容量为 " + intBuffer.capacity() + ", 但是 limit = " + intBuffer.limit());
        }

        // 正确：内部并不是真正的清除数据，只是初始化了相关索引标识
        intBuffer.clear();
        // 存放数据
        for (int i = 0; i < intBuffer.capacity(); i++) {
            intBuffer.put(i * 3);
        }
        // 读写切换
        intBuffer.flip();
        for (int i = 0; i < intBuffer.limit(); i++) {
            System.out.print(intBuffer.get() + " ");
        }
        System.out.println();
    }

    private static void putAndGet() {
        try {
            // put / get 要对应，否则 get 到的数据错误，甚至可能抛异常
            ByteBuffer buffer = ByteBuffer.allocate(64);
            buffer.putInt(100);
            buffer.putLong(123L);
            buffer.putChar('H');
            buffer.putShort((short) 456);

            buffer.flip();
            System.out.println(buffer.getLong());
            System.out.println(buffer.getLong());
            System.out.println(buffer.getChar());
            System.out.println(buffer.getShort());
        } catch (BufferUnderflowException e) {
            // 上面多 get 一次，也是不对应的一种
            System.out.println("put / get 不对应");
        }
    }

    private static void readOnly() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(64);
            buffer.putInt(100);
            buffer.putInt(123);
            ByteBuffer roBuffer = buffer.asReadOnlyBuffer();
            buffer.putInt(456);
            roBuffer.flip();
            while (roBuffer.hasRemaining()) {
                System.out.println("ro=" + roBuffer.getInt());
            }
            // 只读 buffer 不能获取 array，也不能写入数据
            // final byte[] array = roBuffer.array();
            roBuffer.putInt(123);
        } catch (ReadOnlyBufferException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Buffer 有很多实现，常用的是 ByteBuffer
        basicTest();
        System.out.println("==========");
        Thread.sleep(100);

        putAndGet();
        System.out.println("==========");
        Thread.sleep(100);

        readOnly();
    }

}
