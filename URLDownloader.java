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

	private void init(String file, String url) {
		int code = 0;
		try {
			this.url = new URL(url);
			this.file = new File(file);
			HttpURLConnection conn = (HttpURLConnection) this.url.openConnection();
			conn.setRequestMethod("HEAD");
			conn.setRequestProperty("Range", "bytes=0-");
			conn.connect();

			code = conn.getResponseCode();
			length = conn.getContentLength();
			System.out.println("" + length);
			System.out.println("Status: " + code);

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
			
			if (length < 1)
				throw new RuntimeException("???");
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
		RandomAccessFile raf = null;

		public DownTh(int n) {
			this.n = n;
		}

		public void run() {
			try {
				raf = new RandomAccessFile(file, "rw");
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
				byte[] buf = new byte[1024 * 1024];
				InputStream is = conn.getInputStream();
				while ((bn = is.read(buf, 0, 1024 * 1024)) != 0 && bn != -1) {
					raf.write(buf, 0, bn);
					updateProgress(bn);
					Thread.sleep(1);
				}
			} catch (InterruptedException e) {
				// stop
			} catch (Exception e) {
				// retry
				System.out.println(Thread.currentThread().getName() + " -> " + e.getMessage());
				run();
			} finally {
				try { conn.disconnect(); } catch (Exception e) {}
				try { raf.close(); } catch (Exception e) {}
			}
		}
	}
}
