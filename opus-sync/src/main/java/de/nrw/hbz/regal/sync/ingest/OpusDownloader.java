/*
 * Copyright 2012 hbz NRW (http://www.hbz-nrw.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.nrw.hbz.regal.sync.ingest;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import archive.fedora.XmlUtils;

/**
 * 
 * 
 * http://193.30.112.23:9280/fedora/get/dipp:1001?xml=true
 * http://193.30.112.23:9280/fedora/listDatastreams/dipp:1001?xml=true
 * http://193.30.112.23:9280/fedora/get/dipp:1001/DiPPExt
 * 
 * <p>
 * <em>Title: </em>
 * </p>
 * <p>
 * Description:
 * </p>
 * 
 * @author Jan Schnasse, schnasse@hbz-nrw.de
 * 
 */
public class OpusDownloader extends Downloader {

	protected void downloadObject(File dir, String pid) {
		try {
			// pid = pid.replace(':', '-');
			downloadXMetaDissPlus(dir, pid);
			downloadPdfs(dir, pid);

		} catch (MalformedURLException e) {
			logger.error(e.getMessage());

		} catch (IOException e) {
			logger.warn("", e);
		}

	}

	private void downloadXMetaDissPlus(File dir, String pid) throws IOException {

		String url = server + pid;
		logger.info("Download: " + url);
		URL dataStreamUrl = new URL(url);
		File dataStreamFile = new File(dir.getAbsolutePath() + File.separator + "" + pid + ".xml");
		// dataStreamFile.createNewFile();

		logger.info("Save: " + dataStreamFile.getAbsolutePath());

		String data = null;
		StringWriter writer = new StringWriter();
		IOUtils.copy(dataStreamUrl.openStream(), writer);
		data = writer.toString();
		FileUtils.writeStringToFile(dataStreamFile, data, "utf-8");
	}

	private void downloadPdfs(File dir, String pid) {
		Vector<String> files = new Vector<String>();
		String identifier = null;
		Element xMetaDissPlus = XmlUtils.getDocument(new File(dir.getAbsolutePath() + File.separator + pid + ".xml"));

		NodeList identifiers = xMetaDissPlus.getElementsByTagName("ddb:identifier");

		for (int i = 0; i < identifiers.getLength(); i++) {
			Element id = (Element) identifiers.item(i);
			identifier = id.getTextContent();
		}

		NodeList transferUrls = xMetaDissPlus.getElementsByTagName("ddb:transfer");

		if (transferUrls == null || transferUrls.getLength() == 0) {
			// opus 3.2 hbz slang
			NodeList fileProperties = xMetaDissPlus.getElementsByTagName("ddb:fileProperties");

			for (int i = 0; i < fileProperties.getLength(); i++) {
				Element fileProperty = (Element) fileProperties.item(i);
				String filename = fileProperty.getAttribute("ddb:fileName");
				files.add(filename);
			}

			for (int i = 0; i < fileProperties.getLength(); i++) {
				Element fileProperty = (Element) fileProperties.item(i);
				String filename = fileProperty.getAttribute("ddb:fileName");
				files.add(filename);
			}

			int i = 0;
			for (String file : files) {

				if (file.endsWith("pdf")) {
					i++;
					download(new File(dir.getAbsoluteFile() + File.separator + pid + "_" + i + ".pdf"),
							identifier + "/pdf/" + file);
				}
			}
		} else {// qucosa slang
			for (int i = 0; i < transferUrls.getLength(); i++) {
				Element transferUrl = (Element) transferUrls.item(i);
				String url = transferUrl.getTextContent();
				download(new File(dir.getAbsoluteFile() + File.separator + pid + "_" + (i + 1) + ".pdf"), url);
			}
		}
	}

	/**
	 * @param argv
	 *            the argument vector must contain exactly one item which points
	 *            to a valid property file
	 */
	public static void main(String[] argv) {
		if (argv.length != 1) {
			System.out.println("\nWrong Number of Arguments!");
			System.out.println("Please specify a config.properties file!");
			System.out.println("Example: java -jar dtldownloader.jar dtldownloader.properties\n");
			System.out.println(
					"Example Properties File:\n\tpidreporter.server=http://urania.hbz-nrw.de:1801/edowebOAI/\n\tpidreporter.set=null\n\tpidreporter.harvestFromScratch=true\n\tpidreporter.pidFile=pids.txt\n\tpiddownloader.server=http://klio.hbz-nrw.de:1801\n\tpiddownloader.downloadLocation=/tmp/zbmed");
			System.exit(1);
		}

		OpusDownloader main = new OpusDownloader();

		main.run(argv[0]);

	}

}
