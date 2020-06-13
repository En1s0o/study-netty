package eniso.nio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class NIOChannel {

    private static void write01() {
        // 使用 Channel，将数据写入文件示例
        // 创建一个文件输出流，并从中得到 Channel
        try (FileOutputStream fos = new FileOutputStream("channel-01.txt");
             FileChannel channel = fos.getChannel()) {
            // 对 channel 的操作，就是对文件输出流的操作
            /*
            // 在 NIO 中，强制使用 Buffer，所以这里需要创建一个 Buffer
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.put("hello, channel test 01".getBytes());
            // 读写切换
            buffer.flip();
            channel.write(buffer);
            */
            // 在本示例中，可以使用 ByteBuffer.wrap 减少代码量
            channel.write(ByteBuffer.wrap("hello, channel test 01".getBytes()));
            System.out.println("写入完毕");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void read01() {
        // 使用 Channel，从文件读取数据示例
        // 创建一个文件输入流，并从中得到 Channel
        try (FileInputStream fis = new FileInputStream("channel-01.txt");
             FileChannel channel = fis.getChannel()) {
            // 创建一个 Buffer
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            // 通过 Channel 的 read 方法，从 Channel 中读取数据，并写入到 buffer 中
            final int read = channel.read(buffer);
            // 读写切换
            buffer.flip();
            System.out.println("读取到的数据为：" + new String(buffer.array(), 0, read));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copy01() {
        // 使用一个 Buffer 完成文件拷贝
        try (FileInputStream fis = new FileInputStream("channel-01.txt");
             FileOutputStream fos = new FileOutputStream("channel-02.txt");
             final FileChannel inputChannel = fis.getChannel();
             final FileChannel outputChannel = fos.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(7);
            while (true) {
                // 清空 Buffer，实际上数据并没有真正清除，只是重置标识
                // 如果没有 clear，后面的 read 会因为 position == limit，而返回 0，无限循环
                buffer.clear();
                // 从 Channel 中读取数据，写入到 Buffer 中
                final int read = inputChannel.read(buffer);
                System.out.println("读取到 " + read + " 字节");
                if (read == -1) {
                    break;
                }
                // 读写切换
                buffer.flip();
                // 将 Buffer 的数据写入到 Channel 中
                outputChannel.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void zeroCopyTransferFrom() {
        // 0 拷贝
        try (FileInputStream fis = new FileInputStream("channel-01.txt");
             FileOutputStream fos = new FileOutputStream("channel-transferFrom.txt");
             final FileChannel inputChannel = fis.getChannel();
             final FileChannel outputChannel = fos.getChannel()) {
            // 只要操作系统支持，transferFrom 是 0 拷贝的（即：无 CPU 拷贝，或者 CPU 拷贝可以忽略不计）
            long total = inputChannel.size();
            long position = 0;
            while (position < total) {
                long transferCount = outputChannel.transferFrom(inputChannel, position, total - position);
                position += transferCount;
                System.out.println(">>> total=" + total + ", position=" + position + ", transferCount=" + transferCount);
            }
            System.out.println(">>> total=" + total + ", position=" + position);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void zeroCopyTransferTo() {
        // 0 拷贝
        try (FileInputStream fis = new FileInputStream("channel-01.txt");
             FileOutputStream fos = new FileOutputStream("channel-transferTo.txt");
             final FileChannel inputChannel = fis.getChannel();
             final FileChannel outputChannel = fos.getChannel()) {
            // 只要操作系统支持，transferTo 是 0 拷贝的（即：无 CPU 拷贝，或者 CPU 拷贝可以忽略不计）
            long total = inputChannel.size();
            long position = 0;
            while (position < total) {
                long transferCount = inputChannel.transferTo(position, total - position, outputChannel);
                position += transferCount;
                System.out.println(">>> total=" + total + ", position=" + position + ", transferCount=" + transferCount);
            }
            System.out.println(">>> total=" + total + ", position=" + position);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        write01();
        read01();
        copy01();
        zeroCopyTransferFrom();
        zeroCopyTransferTo();
    }

}
