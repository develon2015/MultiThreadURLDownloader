import java.util.*;
import java.net.*;
import java.io.*;
import java.util.regex.*;

class URLDownloader {
	URL url;
	File file;
	double i = 0;
	long length;
	long count;
	int n;

	static {
		System.setProperty("http.agent", "Chrome");
	}

	public URLDownloader(String file, String url) {
		init(file, url);
	}

	/** 原始方法返回值为 int, 无法下载 2G 以上文件
	 */
	public long getContentLength(HttpURLConnection conn) {
		while (true) {
			String header = conn.getHeaderField("Content-Length");
			if (header == null)
				return -2;
			return Long.parseLong(header);
		}
	}

	private void init(String file, String url) {
		int code = 0;
		try {
			this.url = new URL(url);
			this.file = new File(file);
			HttpURLConnection conn = (HttpURLConnection) this.url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Range", "bytes=0-");
			conn.connect();

			code = conn.getResponseCode();
			//length = conn.getContentLength();
			length = getContentLength(conn);
			System.out.println("Status: " + code);
			System.out.println("Length: " + length);

			switch (code) {
			case 206:
				break;
			case 301:
			case 302:
				System.out.println("重定向: " + conn.getHeaderField("Location"));
				conn.disconnect();
				init(file, conn.getHeaderField("Location"));
				break;
			default:
				throw new RuntimeException("不支持" + code);
			}

			conn.disconnect();
			
			if (length < 1) {
				conn = (HttpURLConnection) this.url.openConnection();
				conn.setRequestMethod("GET");
				conn.connect();
				length = conn.getContentLength();
				System.out.println("Length: " + length);
				conn.disconnect();
				if (length < 1) {
					throw new RuntimeException("无法获取大小信息(" + length);
				}
			}
		} catch(Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	List<Thread> ths = new LinkedList<Thread>();
	public void start(int n) {
		this.n = n;
		if (!file.exists()) {
			try {
				if (!file.createNewFile()) {
					throw new RuntimeException("Con't create new file");
				}
			} catch(IOException e) {
					throw new RuntimeException("IO ERROR");
			}
		}
		for (int i = 0; i < n; i ++ ) {
			Thread th = new Thread(new DownTh(i));
			th.setName("下载线程" + i);
			ths.add(th);
			th.start();
		}
	}

	private synchronized void updateProgress(int n) {
		count += n;
		i = (double) count / length * 100;
	}

	public boolean isFinished() {
		if (i >= 100.0)
			return true;
		return false;
	}

	private long old_count;
	private long lastQueryTime;
	private long adding = 0;
	public String toString() {
		String progress = "Total: %.2f %%, Speed: %d KB/s";
		long curTime = System.currentTimeMillis();
		if ((curTime - lastQueryTime) > 999) {
			adding = count - old_count;
			old_count = count;
			lastQueryTime = curTime;
		}
		progress = String.format(progress, i, adding / 1024);
		return progress;
	}

	public void stop() {
		for (int i = 0; i < n; i ++ ) {
			ths.get(i).interrupt();
		}
	}

	class DownTh implements Runnable {
		int n = 0; // 子线程编号
		HttpURLConnection conn = null;
		BufferedRandomAccessFile raf = null;

		public DownTh(int n) {
			this.n = n;
		}

		public void run() {
			InputStream is = null;
			try {
				raf = new BufferedRandomAccessFile(file, "rw", 1024 * 1024 * 20);
				conn = (HttpURLConnection) url.openConnection();
				String range = String.format("bytes=%d-%d",
					 n * (length / URLDownloader.this.n),
					 n == URLDownloader.this.n - 1 ? length - 1 : ((n + 1) * (length / URLDownloader.this.n) - 1));
				conn.setRequestProperty("Range", range);
				System.out.printf("%s -> %s\n", Thread.currentThread().getName(), range);
				conn.connect();
				raf.seek(n * (length / URLDownloader.this.n));

				// save
				int bn = 0;
				int bufsize = 1024 * 1024 * 2;
				byte[] buf = new byte[bufsize];
				//is = new BufferedInputStream(conn.getInputStream(), 1024 * 1024 * 2);
				is = conn.getInputStream();
				while ((bn = is.read(buf, 0, bufsize)) > 0) {
					raf.write(buf, 0, bn);
					updateProgress(bn);
					Thread.sleep(1);
				}
				System.out.println(Thread.currentThread().getName() + "下载完成");
			} catch (InterruptedException e) {
				// stop
			} catch (Exception e) {
				// retry
				System.out.println(Thread.currentThread().getName() + " -> " + e.getMessage());
				run();
			} finally {
				try { raf.flush(); } catch (Exception e) {}
				try { raf.close(); } catch (Exception e) {}
				try { is.close(); } catch (Exception e) {}
				try { conn.disconnect(); } catch (Exception e) {}
			}
		}
	}
}
