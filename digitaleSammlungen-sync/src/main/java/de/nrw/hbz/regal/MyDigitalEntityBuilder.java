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
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import de.nrw.hbz.regal.sync.extern.DigitalEntity;
import de.nrw.hbz.regal.sync.extern.DigitalEntityBuilderInterface;
import de.nrw.hbz.regal.sync.extern.DigitalEntityRelation;
import de.nrw.hbz.regal.sync.extern.RelatedDigitalEntity;
import de.nrw.hbz.regal.sync.extern.StreamType;
import de.nrw.hbz.regal.sync.extern.XmlUtils;

public class MyDigitalEntityBuilder implements DigitalEntityBuilderInterface {

    final static Logger logger = LoggerFactory
	    .getLogger(MyDigitalEntityBuilder.class);

    @Override
    public DigitalEntity build(String baseDir, String pid) throws Exception {
	logger.info(pid + " build DigitalEntity in " + baseDir);
	File metsFile = new File(baseDir + File.separator + pid + ".xml");
	DigitalEntity entity = new DigitalEntity(baseDir);
	entity.setPid(pid);
	loadImages(baseDir, metsFile, entity);
	loadMetsFile(metsFile, entity);
	return entity;
    }

    private void loadImages(String baseDir, File metsFile, DigitalEntity entity) {
	try {
	    Element root = XmlUtils.getDocument(metsFile);
	    List<Element> files = XmlUtils.getElements("//mets:file", root,
		    new MetsNamespaceContext());
	    int i = 0;
	    for (Element element : files) {
		i++;
		String mimeType = element.getAttribute("MIMETYPE");
		Element flocat = (Element) element.getElementsByTagName(
			"mets:FLocat").item(0);
		String url = flocat.getAttribute("xlink:href");
		logger.info("Add as stream: " + url);
		URL dataStreamUrl = new URL(url);
		File file = new File(baseDir + File.separator
			+ dataStreamUrl.getFile());

		DigitalEntity imageEntity = new DigitalEntity(baseDir);
		imageEntity.setPid(entity.getPid() + "_" + i);
		imageEntity.setParentPid(entity.getPid());
		imageEntity.addStream(file, mimeType, StreamType.DATA, url);
		entity.addRelated(new RelatedDigitalEntity(imageEntity,
			DigitalEntityRelation.part_of.toString()));

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

    private void loadMetsFile(File metsFile, DigitalEntity entity) {
	entity.addStream(metsFile, "application/xml", StreamType.DATA);
    }

    @SuppressWarnings("javadoc")
    public class XmlException extends RuntimeException {

	public XmlException() {
	}

	public XmlException(String message) {
	    super(message);
	}

	public XmlException(Throwable cause) {
	    super(cause);
	}

	public XmlException(String message, Throwable cause) {
	    super(message, cause);
	}

    }

    @SuppressWarnings("javadoc")
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
