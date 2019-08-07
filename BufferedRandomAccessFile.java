import java.io.*;

/**
 * 带缓冲的文件随机写
 */
public class BufferedRandomAccessFile extends RandomAccessFile {
	private int bufsize;
	private int n; // 已缓冲长度
	private byte[] buf;

	public BufferedRandomAccessFile(File file, String mode, int bufsize) throws FileNotFoundException {
		super(file, mode);
		this.bufsize = bufsize;
		init();
	}

	public BufferedRandomAccessFile(String filename, String mode, int bufsize) throws FileNotFoundException {
		super(filename, mode);
		this.bufsize = bufsize;
		init();
	}

	private void init() {
		buf = new byte[bufsize];
	}

	public void flush() throws IOException {
		super.write(buf, 0, n);
		n = 0;
	}

	/**
	 * 将 len 个字节从指定 byte 数组写入到此文件，并从偏移量 off 处开始。
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (b == null || len < 1)
			return;
		if (len > bufsize || (n + len) > bufsize) {
			byte[] count = new byte[len + n];
			System.arraycopy(buf, 0, count, 0, n);
			System.arraycopy(b, off, count, n, len);
			super.write(count, 0, count.length);
			n = 0;
			return;
		}
		// buffered
		System.arraycopy(b, off, buf, n, len);
		n += len;
	}

	/**
	 * 设置到此文件开头测量到的文件指针偏移量，在该位置发生下一个读取或写入操作。
	 */
	@Override
	public void seek(long pos) throws IOException {
		flush();
		super.seek(pos);
	}

	@Override
	public void close() throws IOException {
		flush();
		super.close();
	}
}
