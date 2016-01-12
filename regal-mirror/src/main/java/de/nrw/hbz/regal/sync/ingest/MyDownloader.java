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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.nrw.hbz.regal.sync.ingest.Downloader;

/**
 * @author raul
 *
 */
public class MyDownloader extends Downloader {

	final static Logger logger = LoggerFactory.getLogger(MyDownloader.class);

	@Override
	protected void downloadObject(File downloadDirectory, String pid) {
		logger.info("Download " + pid + " to " + downloadDirectory.getAbsolutePath());
		Collector col = new Collector(downloadDirectory, pid);
		// https://api.ellinet-dev.hbz-nrw.de/resource/"+pid+"/all.json?style=long
		// und die pdfs
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

		Downloader main = new MyDownloader();

		main.run(argv[0]);

	}

}
