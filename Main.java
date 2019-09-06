import java.util.*;
import java.net.*;
import java.io.*;
import java.util.regex.*;

public class Main {
	// enter 换行, 避免被行冲洗
	public synchronized static void pt(Object obj, boolean enter) {
		try {
			System.out.println(new String(obj.toString().getBytes(), "UTF-8"));
		} catch (Exception e) {}
		if (enter)
			System.out.println();
	}

	public static void pt(Object obj) {
		pt(obj, false);
	}

	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		System.out.println("多线程下载器V19.0905.1");
		System.out.println("下载地址:");
		String url = scan.nextLine();
		if (!url.matches("^http.*")) {
			url = "http://" + url;
			pt(url);
		}

		System.out.println("线程数量:");
		int n = scan.nextInt();
		Pattern pattern = Pattern.compile(".+/([^\\?]+)(\\?.*)?");
		Matcher matcher = pattern.matcher(url);
		String fn = null;
		if (!matcher.matches()) {
			pt("保存为:");
			fn = scan.nextLine();
		} else {
			fn = matcher.group(1);
		}
		pt("是否保存为: '" + matcher.group(1) + "' ?(Y/n)");
		scan.nextLine();
		switch (scan.nextLine()) {
			case "n":
			case "N":
				pt("保存为:");
				fn = scan.nextLine();
				break;
			default:
		}

		scan.close();
		URLDownloader downloader = new URLDownloader(fn, url);
		downloader.start(n);
		try { Thread.sleep(100); } catch(Exception e) {}
		pt("下载进度:\n");
		pt("");
		while (true) {
			if (downloader.isFinished())
				break;
			//System.err.println("\033[F\033[2K" + downloader);
			pt("\033[F\033[2K" + downloader);
			try {
				Thread.sleep(100);
			} catch(Exception e) {}
		}
		pt("下载完成");
		downloader.stop();
	}
}
