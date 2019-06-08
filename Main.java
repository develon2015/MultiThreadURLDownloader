import java.util.*;
import java.net.*;
import java.io.*;
import java.util.regex.*;

public class Main {
	private static void pt(Object obj) {
		try {
			System.out.println(new String(obj.toString().getBytes(), "UTF-8"));
		} catch(Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		System.out.println("下载地址:");
		String url = scan.nextLine();
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
		while (true) {
			if (downloader.isFinished())
				break;
			System.out.println("\033[F\033[2K" + downloader);
			try {
				Thread.sleep(100);
			} catch(Exception e) {}
		}
		pt("下载完成");
		downloader.stop();
	}
}
