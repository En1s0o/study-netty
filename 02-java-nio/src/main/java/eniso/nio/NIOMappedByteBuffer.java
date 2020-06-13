package eniso.nio;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class NIOMappedByteBuffer {

    public static void main(String[] args) {
        try (RandomAccessFile raf = new RandomAccessFile("mapped-byte-buffer.txt", "rw");
             final FileChannel channel = raf.getChannel()) {
            // MappedByteBuffer 提供直接在堆外内存修改文件的功能，不需要经过操作系统拷贝
            // 映射：采用读写模式，将第 4（下标为 3）个字节开始，大小为 5 个字节的数据映射到内存中
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 3, 5);
            buffer.put(0, (byte) 'M');
            buffer.put(1, (byte) 'N');
            buffer.put(2, (byte) 'O');
            buffer.put(3, (byte) 'P');
            buffer.put(4, (byte) 'Q');
            // buffer.put(5, (byte) 'R'); // ERROR: IndexOutOfBoundsException
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 注意：上面的修改，其实已经生效了，只是 IDEA 工具无法刷新导致数据没有变化
        // 可以在本地采用其他工具打开查看
    }

}
