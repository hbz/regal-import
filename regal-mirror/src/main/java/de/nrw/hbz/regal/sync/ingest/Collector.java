package de.nrw.hbz.regal.sync.ingest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Properties;

import org.json.JSONObject;

public class Collector {

	URL url;
	String pid;
	String userpass;
	MessageDigest md;
	public static Properties prop = new Properties();

	public Collector(File downloadDirectory, String pid) {
		setProp();
		setPid(pid);
		setUserPass();
		setUrl();
		new DataDownloader(downloadDirectory, url, userpass);
	}

	private void setUrl() {
		try {
			url = new URL(prop.getProperty("piddownloader.server") + pid + File.separator
					+ prop.getProperty("resource.style"));
			System.out.println(url);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void setPid(String pid) {
		this.pid = pid;
	}

	private void setUserPass() {
		userpass = prop.getProperty("user") + ":" + prop.getProperty("password");
	}

	private void setProp() {
		try (BufferedInputStream buf = new BufferedInputStream(new FileInputStream(new File(
				Thread.currentThread().getContextClassLoader().getResource("regal-mirror.properties").getPath())))) {
			prop.load(buf);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
