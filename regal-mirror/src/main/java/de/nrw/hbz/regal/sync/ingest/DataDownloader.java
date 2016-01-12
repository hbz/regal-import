package de.nrw.hbz.regal.sync.ingest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.nrw.hbz.regal.ROUtils;

public class DataDownloader {
	URL url;
	String userpass;
	String jsonStr;

	public DataDownloader(File downloadDirectory, URL url, String userpass) {
		this.url = url;
		this.userpass = userpass;
		initConnection();
		setJsonStr();
		initDownload(downloadDirectory);
	}

	private URLConnection initConnection() {
		try {

			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Authorization",
					"Basic " + new String(new Base64().encode(userpass.getBytes())));
			return urlConnection;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private URLConnection initConnection(URL url) {
		try {

			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Authorization",
					"Basic " + new String(new Base64().encode(userpass.getBytes())));
			return urlConnection;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void initDownload(File downloadDirectory) {
		downloadAllPdfs(downloadDirectory, findPdfUrls());
		downloadText(downloadDirectory);
		downloadMetas(downloadDirectory);
	}

	private void downloadMetas(File downloadDirectory) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode json = mapper.readTree(jsonStr);
			String id = json.get("@id").asText().split(":")[1];
			try (OutputStream out = new FileOutputStream(downloadDirectory + File.separator + id + "_metadata.nt");
					InputStream is = initConnection(metadataUrl(url)).getInputStream()) {
				int read = 0;
				byte[] bytes = new byte[1024];
				while ((read = is.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);

		}
	}

	private URL metadataUrl(URL url) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode json = mapper.readTree(jsonStr);
			url = new URL(Collector.prop.getProperty("piddownloader.server") + json.get("@id").asText() + File.separator
					+ "metadata");

			return url;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void setJsonStr() {
		try {
			jsonStr = ROUtils.getStringFromInputStream(initConnection().getInputStream());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void downloadText(File downloadDirectory) {
		try {
			String filename = downloadDirectory.toString();
			filename = filename.split("A")[1] + ".json";
			File downloadDirectoryFile = new File(downloadDirectory + File.separator + filename);
			System.out.println("Textfile >>> " + downloadDirectoryFile);
			ROUtils.stringToJsonFile(jsonStr, downloadDirectoryFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private List<String> findPdfUrls() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode actualObj = mapper.readTree(jsonStr);
			List<JsonNode> list = actualObj.findValues("hasData");
			List<String> dataID = new ArrayList<String>();
			for (int i = 0; i < list.size(); i++) {
				dataID.add(list.get(i).get("@id").asText());
			}
			return dataID;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void downloadAllPdfs(File downloadDirectory, List<String> pdfIds) {
		for (String pdfId : pdfIds) {
			URL pdfUrl = dataUrl(pdfId);
			String downloadLoc = downloadLocation(downloadDirectory, pdfId);
			downloadPdf(pdfUrl, downloadLoc);
		}
	}

	private String downloadLocation(File downloadDirectory, String pdfId) {
		pdfId = pdfId.split("/")[0];
		pdfId = pdfId.split(":")[1];
		pdfId = downloadDirectory + File.separator + pdfId + ".pdf";
		System.out.println(pdfId);
		return pdfId;
	}

	private void downloadPdf(URL pdfUrl, String downloadLoc) {
		try (OutputStream out = new FileOutputStream(new File(downloadLoc));
				InputStream is = initConnection(pdfUrl).getInputStream()) {
			int read = 0;
			byte[] bytes = new byte[1024];
			System.out.println("Download: " + pdfUrl);
			while ((read = is.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			System.out.println("Downloaded: " + pdfUrl);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void downloadPDF() {
		try (OutputStream out = new FileOutputStream(
				new File(Collector.prop.getProperty("piddownloader.downloadLocation") + "/1.pdf"));) {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode actualObj = mapper.readTree(jsonStr);
			List<JsonNode> list = actualObj.findValues("hasData");
			for (int i = 0; i < list.size(); i++) {
				String dataID = list.get(i).get("@id").asText();
				int read = 0;
				byte[] bytes = new byte[1024];
				System.out.println("Download: " + dataID);
				while ((read = initConnection(dataUrl(dataID)).getInputStream().read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
				System.out.println("Downloaded: " + dataID);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private URL dataUrl(String dataID) {
		try {
			URL url = new URL(Collector.prop.getProperty("piddownloader.server") + dataID);
			return url;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
