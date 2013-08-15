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
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nrw.hbz.regal.api.helper.ObjectType;
import de.nrw.hbz.regal.sync.extern.DigitalEntity;
import de.nrw.hbz.regal.sync.extern.RelatedDigitalEntity;
import de.nrw.hbz.regal.sync.extern.Stream;
import de.nrw.hbz.regal.sync.extern.StreamType;
import de.nrw.hbz.regal.sync.extern.XmlUtils;
import de.nrw.hbz.regal.sync.ingest.IngestInterface;
import de.nrw.hbz.regal.sync.ingest.Webclient;

public class MyIngester implements IngestInterface {
    final static Logger logger = LoggerFactory.getLogger(MyIngester.class);

    private String namespace = null;
    String host = null;
    Webclient webclient = null;

    public void init(String host, String user, String password, String ns) {
	this.namespace = ns;
	this.host = host;
	webclient = new Webclient(namespace, user, password, host);
    }

    public void ingest(DigitalEntity dtlBean) {
	String pid = dtlBean.getPid();
	String normalizedPid = pid.replaceAll(":", "-");
	dtlBean.setPid(normalizedPid);
	Stream stream = dtlBean.getStream(StreamType.DATA);
	File metsFile = stream.getFile();
	webclient.createObject(dtlBean, stream.getMimeType(),
		ObjectType.monograph);
	webclient.autoGenerateMetdata(dtlBean);
	webclient.publish(dtlBean);
	Vector<RelatedDigitalEntity> related = dtlBean.getRelated();
	for (RelatedDigitalEntity imageEntity : related) {
	    Stream imageStream = imageEntity.entity.getStream(StreamType.DATA);
	    String oldUrl = imageStream.getFileId();

	    String imageEntityNormalizedPid = imageEntity.entity.getPid()
		    .replaceAll(":", "-");
	    String newUrl = host + "/resource/" + namespace + ":"
		    + imageEntityNormalizedPid + "/data";
	    replaceOldUrls(metsFile, oldUrl, newUrl);
	    imageEntity.entity.setPid(imageEntityNormalizedPid);
	    imageEntity.entity.setParentPid(normalizedPid);
	    webclient.createObject(imageEntity.entity,
		    imageStream.getMimeType(), ObjectType.file);
	}

    }

    private void replaceOldUrls(File metsFile, String oldUrl, String newUrl) {
	try {
	    String oldContent = XmlUtils.fileToString(metsFile);
	    System.out.println(oldUrl + " " + newUrl);
	    oldContent = oldContent.replaceAll(oldUrl, newUrl);
	    XmlUtils.stringToFile(metsFile, oldContent);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void update(DigitalEntity dtlBean) {
	// TODO: implement me
    }

    public void delete(String pid) {
	// TODO: implement me
    }

}
