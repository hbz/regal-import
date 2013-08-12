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
package de.nrw.hbz.regal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import de.nrw.hbz.regal.sync.extern.XmlUtils;
import de.nrw.hbz.regal.sync.ingest.Downloader;

public class MyDownloader extends Downloader {
    final static Logger logger = LoggerFactory.getLogger(MyDownloader.class);

    protected void downloadObject(File dir, String pid) {
	downloadMets(dir, pid);
	downloadImages(dir, pid);
    }

    private void downloadImages(File dir, String pid) {
	File dataStreamFile = new File(dir.getAbsolutePath() + File.separator
		+ "" + pid + ".xml");
	logger.info(pid + " download images.");
	try {
	    Element root = XmlUtils.getDocument(dataStreamFile);
	    List<Element> files = XmlUtils.getElements("//mets:FLocat", root,
		    new MetsNamespaceContext());
	    for (Element element : files) {
		String url = element.getAttribute("xlink:href");
		logger.info("Download: " + url);
		download(dir, url);
	    }
	} catch (FileNotFoundException e) {
	    throw new XmlException(e);
	} catch (ParserConfigurationException e) {
	    throw new XmlException(e);
	} catch (SAXException e) {
	    throw new XmlException(e);
	} catch (IOException e) {
	    throw new XmlException(e);
	} catch (XPathExpressionException e) {
	    throw new XmlException(e);
	}
    }

    private void downloadMets(File dir, String pid) {
	String url = getServer() + pid;
	logger.info("Download: " + url);
	URL dataStreamUrl;
	try {
	    dataStreamUrl = new URL(url);
	    File dataStreamFile = new File(dir.getAbsolutePath()
		    + File.separator + "" + pid + ".xml");
	    logger.info("Save: " + dataStreamFile.getAbsolutePath());
	    String data = null;
	    StringWriter writer = new StringWriter();
	    IOUtils.copy(dataStreamUrl.openStream(), writer);
	    data = writer.toString();
	    FileUtils.writeStringToFile(dataStreamFile, data, "utf-8");
	} catch (MalformedURLException e) {
	    throw new DownloadException(e);
	} catch (IOException e) {
	    throw new DownloadException(e);
	}

    }

    private void download(File dir, String url) throws IOException {
	URL dataStreamUrl = new URL(url);
	File file = new File(dir.getAbsolutePath() + File.separator
		+ dataStreamUrl.getFile());
	logger.info("Download target: " + file.getAbsolutePath());
	String data = null;
	StringWriter writer = new StringWriter();
	IOUtils.copy(dataStreamUrl.openStream(), writer);
	data = writer.toString();
	FileUtils.writeStringToFile(file, data, "utf-8");
    }

    public class DownloadException extends RuntimeException {

	public DownloadException() {
	}

	public DownloadException(String arg0) {
	    super(arg0);
	}

	public DownloadException(Throwable arg0) {
	    super(arg0);
	}

	public DownloadException(String arg0, Throwable arg1) {
	    super(arg0, arg1);
	}

    }

    public class XmlException extends RuntimeException {

	public XmlException() {
	    // TODO Auto-generated constructor stub
	}

	public XmlException(String message) {
	    super(message);
	    // TODO Auto-generated constructor stub
	}

	public XmlException(Throwable cause) {
	    super(cause);
	    // TODO Auto-generated constructor stub
	}

	public XmlException(String message, Throwable cause) {
	    super(message, cause);
	    // TODO Auto-generated constructor stub
	}

    }

    public class MetsNamespaceContext implements NamespaceContext {

	public String getNamespaceURI(String prefix) {
	    if (prefix == null)
		throw new NullPointerException("Null prefix");
	    else if ("mets".equals(prefix))
		return "http://www.loc.gov/METS/";
	    else if ("xml".equals(prefix))
		return XMLConstants.XML_NS_URI;
	    return XMLConstants.NULL_NS_URI;
	}

	// This method isn't necessary for XPath processing.
	public String getPrefix(String uri) {
	    throw new UnsupportedOperationException();
	}

	// This method isn't necessary for XPath processing either.
	public Iterator getPrefixes(String uri) {
	    throw new UnsupportedOperationException();
	}

    }

}
