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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nrw.hbz.regal.sync.extern.DigitalEntity;
import de.nrw.hbz.regal.sync.extern.RelatedDigitalEntity;
import de.nrw.hbz.regal.sync.ingest.IngestInterface;
import de.nrw.hbz.regal.sync.ingest.KeystoreConf;
import de.nrw.hbz.regal.sync.ingest.Webclient;
import models.ObjectType;

public class MyIngester implements IngestInterface {
	final static Logger logger = LoggerFactory.getLogger(MyIngester.class);

	private String namespace = null;
	String host = null;
	Webclient webclient = null;

	@Override
	public void init(String host, String user, String password, String ns, KeystoreConf kconf) {
		System.out.println("Init MyIngester " + user + " " + password + " " + ns);
		this.namespace = ns;
		this.host = host;
		webclient = new Webclient(namespace, user, password, host, null);
	}

	public void ingest(DigitalEntity dtlBean) {
		System.out.println("Ingest: " + dtlBean.getPid() + " of type " + dtlBean.getType());
		createObject(dtlBean);
	}

	private void createObject(DigitalEntity dtlBean, ObjectType t) {
		// webclient.createResource(t, dtlBean);

		/// Ein Objekt im Repository anlegen mit apache HttpClient oder jersey
		/// Client
		// PUT /resource/:pid -d{"contentType": dtlBean.getType,
		/// "accessScheme":"....","publishScheme":"....","parentPid":""}
		// PUT /resource/:pid/metadata

		for (RelatedDigitalEntity e : dtlBean.getRelated()) {
			DigitalEntity nextEntity = e.entity;
			createObject(nextEntity);
		}
	}

	private void createObject(DigitalEntity dtlBean) {
		if ("monograph".equals(dtlBean.getType())) {
			createObject(dtlBean, ObjectType.monograph);
		} else if ("issue".equals(dtlBean.getType())) {
			createObject(dtlBean, ObjectType.issue);
		} else if ("journal".equals(dtlBean.getType())) {
			createObject(dtlBean, ObjectType.journal);
		} else if ("volume".equals(dtlBean.getType())) {
			createObject(dtlBean, ObjectType.volume);
		}
	}

	public void update(DigitalEntity dtlBean) {
		// TODO: implement me
	}

	public void delete(String pid) {
		// TODO: implement me
	}

	@Override
	public void test() {
		// TODO Auto-generated method stub

	}

}
