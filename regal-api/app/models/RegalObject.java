/*
 * Copyright 2014 hbz NRW (http://www.hbz-nrw.de/)
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
package models;

import java.util.List;
import java.util.Vector;

/**
 * @author Jan Schnasse schnasse@hbz-nrw.de
 * 
 */
public class RegalObject {

    String type = null;
    String parentPid = null;
    List<String> transformer = null;
    List<String> indexes = null;
    String accessScheme = null;

    /**
     * Default constructor
     * 
     */
    public RegalObject() {
	transformer = new Vector<String>();
	indexes = new Vector<String>();
	type = "not";
    }

    /**
     * @return all Transformer-Ids
     */
    public List<String> getTransformer() {
	return transformer;
    }

    /**
     * @param t
     *            list of Transformer-Ids
     */
    public void setTransformer(List<String> t) {
	transformer = t;
    }

    /**
     * @param t
     *            a valid type
     */
    public RegalObject(ObjectType t) {
	type = t.toString();
    }

    /**
     * @return the type of the object
     */
    public String getType() {
	return type;
    }

    /**
     * @param type
     *            the type
     */
    public void setType(String type) {
	this.type = type;
    }

    /**
     * @return the parent
     */
    public String getParentPid() {
	return parentPid;
    }

    /**
     * @param parentPid
     *            the parent
     */
    public void setParentPid(String parentPid) {
	this.parentPid = parentPid;
    }

    /**
     * @return a list of indexes, that are updated on create/modify
     */
    public List<String> getIndexes() {
	return indexes;
    }

    /**
     * @param indexes
     *            a list of indexes, that are updated on create/modify, valid
     *            values so far: null, public, private
     */
    public void setIndexes(List<String> indexes) {
	this.indexes = indexes;
    }

    /**
     * @return a string that signals who is allowed to access this node
     */
    public String getAccessScheme() {
	return accessScheme;
    }

    /**
     * @param accessScheme
     *            a string that signals who is allowed to access this node
     */
    public void setAccessScheme(String accessScheme) {
	this.accessScheme = accessScheme;
    }

}
