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
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.nrw.hbz.regal.sync.extern.DigitalEntity;
import de.nrw.hbz.regal.sync.extern.DigitalEntityBuilderInterface;

public class MyDigitalEntityBuilder implements DigitalEntityBuilderInterface {

	public DigitalEntity build(String baseDir, String pid) {
		try {
			System.out.println("Build Entity: " + baseDir);
			return createObject(baseDir, pid);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private DigitalEntity createObject(String baseDir, String pid)
			throws IOException, JsonParseException, JsonMappingException {
		String id = pid.substring(pid.length() - 7);
		Map<String, Object> object = new ObjectMapper().readValue(new File(baseDir + File.separator + id + ".json"),
				Map.class);
		return createObject(object);
	}

	private DigitalEntity createObject(Map<String, Object> object) {
		DigitalEntity result = new DigitalEntity("");
		result.setPid((String) object.get("@id"));
		result.setType((String) object.get("contentType"));
		List<Map<String, Object>> list = (List<Map<String, Object>>) object.get("hasPart");
		if (list == null) {
			for (Map<String, Object> nextObject : list) {
				String onlyKey = nextObject.keySet().iterator().next();
				Map<String, Object> actualObject = (Map<String, Object>) nextObject.get(onlyKey);
				System.out.println("Part: " + actualObject);
				DigitalEntity child = createObject(actualObject);
				child.setParentPid(result.getPid());
				result.addRelated(child, "hasPart");
			}
		}
		return result;
	}

}
