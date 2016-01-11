package de.nrw.hbz.regal;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.nrw.hbz.regal.sync.ingest.Collector;
import de.nrw.hbz.regal.sync.ingest.KeystoreConf;
import de.nrw.hbz.regal.sync.ingest.Webclient;

public class DownloadTest {

	static KeystoreConf conf = new KeystoreConf();
	static String user = "edoweb-anonymous";
	static String password = "admin";
	static URL url;
	static URL url2;
	static String basicAuth;
	static Webclient webclient;
	static URLConnection urlConnection;
	static URLConnection urlConnection2;

	@BeforeClass
	public static void initConfigs() throws IOException {
		try {
			conf.location = "/opt/jdk/jre/lib/security/cacerts";
			conf.password = "changeit";
			url = new URL("https://api.ellinet-dev.hbz-nrw.de/resource/frl:6376984/all.json?style=long");
			url2 = new URL("https://api.ellinet-dev.hbz-nrw.de/resource/frl:3175693-1/data");
			url.openConnection();
			urlConnection = url.openConnection();
			url2.openConnection();
			urlConnection2 = url2.openConnection();
			String userpass = user + ":" + password;
			basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
			urlConnection.setRequestProperty("Authorization", basicAuth);
			webclient = new Webclient("frl", user, password, "api.ellinet-dev.hbz-nrw.de", conf);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void dataReaderTest() throws IOException {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			URL url2 = new URL("https://api.ellinet-dev.hbz-nrw.de/resource/frl:3175693-1/data");
			url2.openConnection();
			URLConnection uc = url2.openConnection();
			String userpass = user + ":" + password;
			String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
			uc.setRequestProperty("Authorization", basicAuth);
			InputStream in = uc.getInputStream();
			OutputStream out = new FileOutputStream(new File("/tmp/1.pdf"));
			DigestInputStream dis = new DigestInputStream(in, md);
			byte[] dataBytes = new byte[1024];
			int nread = 0;

			while ((nread = in.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			}
			;
			byte[] mdbytes = md.digest();

			StringBuffer sb = new StringBuffer("");
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}

			System.out.println("Digest(in hex format):: " + sb.toString());
			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = in.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void collector_Test() {
		Collector col = new Collector(new File("/home/raul/test/t1"), "frl:6376984");

	}

	@Test
	public void writerTest() {

		try {
			FileWriter fw = new FileWriter("/tmp/1.txt");
			String line = ROUtils.getStringFromInputStream(urlConnection.getInputStream());
			System.out.println(line);
			String jsonObj = new JSONObject(line).toString(2);
			fw.write(jsonObj);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void writeer2Test() {
		try {
			String jsonStr = ROUtils.getStringFromInputStream(urlConnection.getInputStream());
			File file = new File("/tmp/2.txt");
			ROUtils.stringToJsonFile(jsonStr, file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void soTest() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader("/tmp/2.txt"));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			String jsonStr = sb.toString();
			ROUtils.stringToJsonFile(jsonStr, new File("/tmp/3.txt"));
		} finally {
			br.close();
		}
	}

	@Test
	public void test_setProp() {
		Properties prop = new Properties();
		try (BufferedInputStream buf = new BufferedInputStream(new FileInputStream(new File(Thread.currentThread()
				.getContextClassLoader().getResource("regal-mirror-test.properties").getPath())))) {
			System.out.println(Thread.currentThread().getContextClassLoader()
					.getResource("regal-mirror-test.properties").getPath());
			prop.load(buf);
			System.out.println(prop.getProperty("user"));
			System.out.println(prop.getProperty("password"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void test_JsonArray() {
		String line;
		try {
			line = ROUtils.getStringFromInputStream(urlConnection.getInputStream());
			JSONObject jsonObj = new JSONObject(line);
			JSONArray jar = jsonObj.getJSONArray("hasPart");
			Iterator<Object> it = jar.iterator();
			while (it.hasNext()) {
				Object o = it.next();
				if (o instanceof JSONObject) {
					JSONObject jso = (JSONObject) o;
					Iterator<String> keys = jso.keys();
					String k = keys.next();
					System.out.println("++++++" + k);

				} else if (o instanceof JSONArray) {
					JSONArray jar1 = (JSONArray) o;

					System.out.println("------" + jar1);
				}

			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public void itrO(JSONObject job) {

		if (job.has("hasData")) {
			job = job.getJSONObject("hasData");
			System.out.println(job.get("@id"));

		} else {
			JSONArray jar = job.getJSONArray("hasPart");
			job = (JSONObject) jar.getJSONObject(0);
			job = job.getJSONObject(job.keys().next());
			itrO(job);
		}

	}

	@Test
	public void jackson_test() throws IOException {
		String jsonString = new String(Files.readAllBytes(
				Paths.get(Thread.currentThread().getContextClassLoader().getResource("json.json").getPath())));

		ObjectMapper mapper = new ObjectMapper();
		JsonNode actualObj = mapper.readTree(jsonString);
		List<JsonNode> list = actualObj.findValues("hasData");
		List<String> dataID = new ArrayList<String>();
		for (int i = 0; i < list.size(); i++) {
			dataID.add(list.get(i).get("@id").asText());
		}
		String s = dataID.get(0);
		s = s.split("/")[0];
		s = s.split(":")[1];
		System.out.println(s);
	}

	@Test
	public void writeer3Test() {
		try {
			String jsonStr = ROUtils.getStringFromInputStream(urlConnection.getInputStream());
			ObjectMapper mapper = new ObjectMapper();
			JsonNode json = mapper.readTree(jsonStr);
			jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
			System.out.println(jsonStr);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void input_Test() {
		try {
			String jsonStr = ROUtils.getStringFromInputStream(initConnection().getInputStream());
			ObjectMapper mapper = new ObjectMapper();
			JsonNode json = mapper.readTree(jsonStr);
			System.out.println(json.get("@id").asText());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public URLConnection initConnection() {
		try {
			String userpass = user + ":" + password;
			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Authorization",
					"Basic " + new String(new Base64().encode(userpass.getBytes())));
			return urlConnection;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
