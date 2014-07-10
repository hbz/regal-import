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
package helper;

import static de.nrw.hbz.regal.datatypes.Vocabulary.REL_CONTENT_TYPE;
import static de.nrw.hbz.regal.datatypes.Vocabulary.REL_IS_NODE_TYPE;
import static de.nrw.hbz.regal.datatypes.Vocabulary.TYPE_OBJECT;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.HAS_PART;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.IS_PART_OF;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import models.DCBeanAnnotated;
import models.RegalObject;

import org.openrdf.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import de.nrw.hbz.regal.datatypes.DCBean;
import de.nrw.hbz.regal.datatypes.Link;
import de.nrw.hbz.regal.datatypes.Node;
import de.nrw.hbz.regal.datatypes.Transformer;
import de.nrw.hbz.regal.exceptions.ArchiveException;
import de.nrw.hbz.regal.fedora.CopyUtils;
import de.nrw.hbz.regal.fedora.FedoraFactory;
import de.nrw.hbz.regal.fedora.FedoraInterface;
import de.nrw.hbz.regal.fedora.FedoraVocabulary;
import de.nrw.hbz.regal.fedora.RdfException;
import de.nrw.hbz.regal.fedora.RdfUtils;
import de.nrw.hbz.regal.fedora.UrlConnectionException;
import de.nrw.hbz.regal.search.SearchFacade;

/**
 * Actions provide a single class to access the archive. All endpoints are using
 * this class.
 * 
 * @author Jan Schnasse, schnasse@hbz-nrw.de
 * 
 */
public class Actions {

    @SuppressWarnings({ "serial" })
    private class UrnException extends RuntimeException {
	public UrnException(String arg0) {
	    super(arg0);
	}

	public UrnException(Throwable arg0) {
	    super(arg0);
	}

    }

    @SuppressWarnings({ "serial" })
    private class UpdateNodeException extends RuntimeException {
	public UpdateNodeException(Throwable cause) {
	    super(cause);
	}
    }

    final static Logger logger = LoggerFactory.getLogger(Actions.class);
    private static Actions actions = null;

    private Services services = null;
    private Representations representations = null;
    private FedoraInterface fedora = null;

    private String fedoraExtern = null;
    private String server = null;
    private String urnbase = null;
    private SearchFacade search = null;
    private String escluster = null;

    // String namespace = null;

    /**
     * @throws IOException
     *             if properties can not be loaded.
     */
    private Actions() throws IOException {
	fedoraExtern = Play.application().configuration()
		.getString("regal-api.fedoraExtern");
	server = Play.application().configuration()
		.getString("regal-api.serverName");
	urnbase = Play.application().configuration()
		.getString("regal-api.urnbase");
	escluster = Play.application().configuration()
		.getString("regal-api.escluster");
	fedora = FedoraFactory.getFedoraImpl(Play.application().configuration()
		.getString("regal-api.fedoraIntern"), Play.application()
		.configuration().getString("regal-api.user"), Play
		.application().configuration().getString("regal-api.password"));
	services = new Services(fedora, server);
	representations = new Representations(fedora, server);
	search = new SearchFacade(escluster);
    }

    /**
     * @return an instance of this Actions.class
     * @throws IOException
     *             if properties can't be read
     */
    public static Actions getInstance() throws IOException {
	if (actions == null)
	    actions = new Actions();
	return actions;
    }

    /**
     * @return the host name
     */
    public String getServer() {
	return server;
    }

    /**
     * @param pids
     *            The pids that must be deleted
     * @return A short message.
     */
    public String deleteAll(List<String> pids) {
	if (pids == null || pids.isEmpty()) {
	    return "Nothing to delete!";
	}
	StringBuffer msg = new StringBuffer();
	for (String pid : pids) {
	    try {
		msg.append(delete(pid) + "\n");
	    } catch (Exception e) {
		logger.warn(pid + " " + e.getMessage());
	    }
	}
	return msg.toString();
    }

    /**
     * @param pid
     *            The pid that must be deleted
     * @return A short Message
     */
    public String delete(String pid) {
	StringBuffer msg = new StringBuffer();
	List<Node> pids = null;
	try {
	    pids = fedora.deleteComplexObject(pid);
	} catch (Exception e) {
	    msg.append("\n" + e);
	}
	try {
	    if (pids != null) {
		for (Node n : pids) {
		    msg.append("\n" + removeIdFromPublicAndPrivateIndex(n));
		}
	    }
	} catch (Exception e) {
	    msg.append("\n" + e);
	}
	return pid + " successfully deleted! \n" + msg + "\n";
    }

    private String removeIdFromPublicAndPrivateIndex(Node n) {
	StringBuffer msg = new StringBuffer();
	try {
	    String namespace = n.getNamespace();
	    String m = removeFromIndex(namespace, n.getContentType(),
		    n.getPID());
	    msg.append("\n" + m);
	    m = removeFromIndex("public_" + namespace, n.getContentType(),
		    n.getPID());
	    msg.append("\n" + m);
	} catch (Exception e) {
	    msg.append("\n" + e);
	}
	return msg.toString();
    }

    /**
     * @param pid
     *            a namespace qualified id
     * @return a message
     */
    public String deleteMetadata(String pid) {

	fedora.deleteDatastream(pid, "metadata");
	index(readNode(pid));
	return pid + ": metadata - datastream successfully deleted! ";
    }

    /**
     * @param pid
     *            the pid og the object
     * @return a message
     */
    public String deleteData(String pid) {
	fedora.deleteDatastream(pid, "data");
	index(readNode(pid));
	return pid + ": data - datastream successfully deleted! ";
    }

    /**
     * @param pid
     *            The pid to read the dublin core stream from.
     * @return A DCBeanAnnotated java object.
     */
    public DCBeanAnnotated readDC(String pid) {
	Node node = fedora.readNode(pid);
	if (node != null)
	    return new DCBeanAnnotated(node);
	return null;
    }

    /**
     * @param pid
     *            the pid of the object
     * @return n-triple metadata
     */
    public String readMetadata(String pid) {
	String metadataAdress = fedoraExtern + "/objects/" + pid
		+ "/datastreams/metadata/content";
	try {
	    return RdfUtils.readRdfToString(new URL(metadataAdress),
		    RDFFormat.NTRIPLES, RDFFormat.NTRIPLES, "text/plain");
	} catch (MalformedURLException e) {
	    throw new HttpArchiveException(500, "Wrong Metadata adress: "
		    + metadataAdress);
	} catch (RdfException e) {
	    throw new HttpArchiveException(500, e);
	} catch (UrlConnectionException e) {
	    throw new HttpArchiveException(404, e);
	}
    }

    /**
     * @param pid
     *            the pid that must be updated
     * @param content
     *            the file content as byte array
     * @param mimeType
     *            the mimetype of the file
     * @param name
     *            the name of the file
     * @param md5Hash
     *            a hash for the content. Can be null.
     * @return A short message
     * @throws IOException
     *             if data can not be written to a tmp file
     */
    public String updateData(String pid, InputStream content, String mimeType,
	    String name, String md5Hash) throws IOException {
	if (content == null) {
	    throw new HttpArchiveException(406, pid
		    + " you've tried to upload an empty stream."
		    + " This action is not supported. Use HTTP DELETE instead.");
	}
	File tmp = File.createTempFile(name, "tmp");
	tmp.deleteOnExit();
	CopyUtils.copy(content, tmp);
	Node node = fedora.readNode(pid);
	if (node != null) {
	    node.setUploadData(tmp.getAbsolutePath(), mimeType);
	    node.setFileLabel(name);
	    node.setMimeType(mimeType);
	    fedora.updateNode(node);
	} else {
	    throw new HttpArchiveException(500, "Lost Node!");
	}
	index(node);
	if (md5Hash != null && !md5Hash.isEmpty()) {
	    node = fedora.readNode(pid);
	    String fedoraHash = node.getChecksum();
	    if (!md5Hash.equals(fedoraHash)) {
		throw new HttpArchiveException(417, pid + " expected a MD5 of "
			+ fedoraHash + " but you provided a MD5 value of "
			+ md5Hash);
	    }
	}
	return pid + " data successfully updated!";
    }

    /**
     * @param pid
     *            The pid that must be updated
     * @param json
     *            dc as json object
     * @param content
     *            A dublin core object
     * @return a short message
     */
    public String updateDC(String pid,
	    com.fasterxml.jackson.databind.JsonNode json) {
	Node node = fedora.readNode(pid);
	DCBean dc = node.getBean();
	dc.setContributer(json.findValuesAsText("contributor"));
	dc.setCoverage(json.findValuesAsText("coverage"));
	dc.setCreator(json.findValuesAsText("creator"));
	dc.setDate(json.findValuesAsText("date"));
	dc.setDescription(json.findValuesAsText("description"));
	dc.setFormat(json.findValuesAsText("format"));
	dc.setIdentifier(json.findValuesAsText("identifier"));
	dc.setLanguage(json.findValuesAsText("language"));
	dc.setPublisher(json.findValuesAsText("publisher"));
	dc.setDescription(json.findValuesAsText("description"));
	dc.setRights(json.findValuesAsText("rights"));
	dc.setSource(json.findValuesAsText("source"));
	dc.setSubject(json.findValuesAsText("subject"));
	dc.setTitle(json.findValuesAsText("title"));
	dc.setType(json.findValuesAsText("type"));
	node.setDcBean(dc);
	fedora.updateNode(node);
	index(node);
	return pid + " dc successfully updated!";
    }

    /**
     * @param pid
     *            The pid that must be updated
     * @param content
     *            The metadata as rdf string
     * @return a short message
     */
    public String updateMetadata(String pid, String content) {
	try {
	    if (content == null) {
		throw new HttpArchiveException(406, pid
			+ "You've tried to upload an empty string."
			+ " This action is not supported."
			+ " Use HTTP DELETE instead.");
	    }
	    RdfUtils.validate(content);
	    File file = CopyUtils.copyStringToFile(content);
	    Node node = fedora.readNode(pid);
	    if (node != null) {
		node.setMetadataFile(file.getAbsolutePath());
		fedora.updateNode(node);
	    }
	    index(node);
	    return pid + " metadata successfully updated!";
	} catch (RdfException e) {
	    throw new HttpArchiveException(400);
	} catch (IOException e) {
	    throw new UpdateNodeException(e);
	}
    }

    /**
     * @param node
     *            read metadata from the Node to the repository
     * @return a message
     */
    public String updateMetadata(Node node) {
	fedora.updateNode(node);
	String pid = node.getPID();
	index(node);
	return pid + " metadata successfully updated!";
    }

    /**
     * @param pid
     *            The pid to which links must be added
     * @param links
     *            list of links
     * @return a short message
     */
    public String addLinks(String pid, List<Link> links) {
	Node node = fedora.readNode(pid);
	for (Link link : links) {
	    node.addRelation(link);
	}
	fedora.updateNode(node);
	index(node);
	return pid + " " + links + " links successfully added.";
    }

    /**
     * @param pid
     *            The pid to which links must be added uses: Vector<Link> v =
     *            new Vector<Link>(); v.add(link); return addLinks(pid, v);
     * @param link
     *            a link
     * @return a short message
     */
    public String addLink(String pid, Link link) {
	Vector<Link> v = new Vector<Link>();
	v.add(link);
	return addLinks(pid, v);
    }

    /**
     * 
     * @param cms
     *            a List of Transformers
     * @return a message
     */
    public String contentModelsInit(List<Transformer> cms) {
	try {
	    fedora.updateContentModels(cms);
	    return "Success!";
	} catch (ArchiveException e) {
	    throw new HttpArchiveException(500, e);
	}
    }

    // /**
    // * @param query
    // * a query to define objects that must be deleted
    // * @return a message
    // */
    // public String deleteByQuery(String query) {
    // List<String> objects = listByQuery(query);
    // return deleteAll(objects);
    // }

    /**
     * @param pid
     *            the pid
     * @return the last modified date
     */
    public Date getLastModified(String pid) {
	Node node = fedora.readNode(pid);
	return node.getLastModified();
    }

    /**
     * @param type
     *            the type of the new resource
     * @param parent
     *            the parent of a new rsource
     * @param transformers
     *            transformers connected to the resource
     * @param accessScheme
     *            a string that signals who is allowed to access this node
     * @param input
     *            the input defines the contenttype and a optional parent
     * @param rawPid
     *            the pid without namespace
     * @param namespace
     *            the namespace
     * @return the Node representing the resource
     */
    public Node createResource(String type, String parent,
	    List<String> transformers, String accessScheme, String rawPid,
	    String namespace) {
	logger.debug("create " + type);
	Node node = createNodeIfNotExists(type, parent, transformers,
		accessScheme, rawPid, namespace);
	removeFromIndex(namespace, node.getContentType(), node.getPID());
	node.setAccessScheme(accessScheme);
	setNodeType(type, node);
	linkWithParent(parent, node);
	updateTransformer(transformers, node);
	fedora.updateNode(node);
	index(node);
	return node;
    }

    private Node createNodeIfNotExists(String type, String parent,
	    List<String> transformers, String accessScheme, String rawPid,
	    String namespace) {
	String pid = namespace + ":" + rawPid;
	Node node = null;
	if (fedora.nodeExists(pid)) {
	    node = fedora.readNode(pid);
	} else {
	    node = new Node();
	    node.setNamespace(namespace).setPID(pid);
	    node.setContentType(type);
	    node.setAccessScheme(accessScheme);
	    fedora.createNode(node);
	}
	return node;
    }

    private void setNodeType(String type, Node node) {
	node.setType(TYPE_OBJECT);
	node.setContentType(type);
	index(node);
    }

    private void linkWithParent(String parentPid, Node node) {
	fedora.unlinkParent(node);
	fedora.linkToParent(node, parentPid);
	fedora.linkParentToNode(parentPid, node.getPID());
	index(node);
    }

    private void updateTransformer(List<String> transformers, Node node) {
	node.removeAllContentModels();
	for (String t : transformers) {
	    node.addTransformer(new Transformer(t));
	}
    }

    /**
     * @param pid
     *            adds lobidmetadata (if avaiable) to the node and updates the
     *            repository
     * @return a message
     */
    public String lobidify(String pid) {
	Node node = fedora.readNode(pid);
	node = services.lobidify(node);
	index(node);
	return updateMetadata(node);
    }

    /**
     * Returns an existing urn. Throws UrnException if found 0 urn or more than
     * 1 urns.
     * 
     * @param pid
     *            the pid of an object
     * @return the urn
     */
    public String getUrn(String pid) {
	try {
	    String metadataAdress = fedoraExtern + "/objects/" + pid
		    + "/datastreams/metadata/content";
	    URL url = new URL(metadataAdress);
	    String newUrn = "http://purl.org/lobid/lv#urn";
	    // String oldUrn =
	    // "http://geni-orca.renci.org/owl/topology.owl#hasURN";
	    List<String> urns = RdfUtils.findRdfObjects(pid, newUrn, url,
		    RDFFormat.NTRIPLES, "text/plain");
	    if (urns == null || urns.isEmpty()) {
		throw new UrnException("Found no urn!");
	    }
	    if (urns.size() != 1) {
		throw new UrnException("Found " + urns.size() + " urns. "
			+ urns + "\n Expected exactly one urn.");
	    }
	    return urns.get(0);
	} catch (Exception e) {
	    throw new UrnException(e);
	}
    }

    /**
     * @param pid
     *            the pid of a node that must be published on the oai interface
     * @return A short message.
     */
    public String makeOAISet(String pid) {
	return services.makeOAISet(pid, fedoraExtern);
    }

    /**
     * @param index
     *            the elasticsearch index
     * @param type
     *            the type of the resource
     * @param pid
     *            The namespaced pid to remove from index
     * @return A short message
     */
    public String removeFromIndex(String index, String type, String pid) {
	search.delete(index, type, pid);
	return pid + " of type " + type + " removed from index " + index + "!";
    }

    /**
     * @param p
     *            The pid with namespace that must be indexed
     * @param index
     *            the name of the index. Convention is to use the namespace of
     *            the pid.
     * @param type
     *            the type of the resource
     * @return a short message.
     */
    public String index(String p, String index, String type) {
	search.init(index, "public-index-config.json");
	String jsonCompactStr = oaiore(p, "application/json+compact");
	search.index(index, type, p, jsonCompactStr);
	return p + " indexed!";
    }

    private String index(Node n) {
	String namespace = n.getNamespace();
	String pid = n.getPID();
	return index(pid, namespace, n.getContentType());
    }

    /**
     * Returns a list of pids of related objects. Looks for other objects those
     * are connected to the pid by a certain relation
     * 
     * @param pid
     *            the pid to find relatives of
     * @param relation
     *            a relation that describes what kind of relatives you are
     *            looking for
     * @return list of pids of related objects
     */
    public List<String> getRelatives(String pid, String relation) {
	List<String> result = new Vector<String>();
	Node node = readNode(pid);
	List<Link> links = node.getRelsExt();
	for (Link l : links) {
	    if (l.getPredicate().equals(relation))
		result.add(l.getObject());
	}
	return result;
    }

    /**
     * @param type
     *            a contentType
     * @param namespace
     *            list only objects in this namespace
     * @param from
     *            show only hits starting at this index
     * @param until
     *            show only hits ending at this index
     * @param getListingFrom
     *            List Resources from elasticsearch or from fedora. Allowed
     *            values: "repo" and "es"
     * @return all objects of contentType type
     */
    public List<String> list(String type, String namespace, int from,
	    int until, String getListingFrom) {
	List<String> list = null;
	if (!"es".equals(getListingFrom)) {
	    list = listRepo(type, namespace, from, until);
	} else {
	    list = listSearch(type, namespace, from, until);
	}
	return list;
    }

    /**
     * @param type
     *            The objectTyp
     * @param namespace
     *            list only objects in this namespace
     * @param from
     *            show only hits starting at this index
     * @param until
     *            show only hits ending at this index
     * @return A list of pids with type {@type}
     */
    public List<String> listSearch(String type, String namespace, int from,
	    int until) {
	return search.listIds(namespace, type, from, until);
    }

    private List<String> listRepo(String type, String namespace, int from,
	    int until) {
	List<String> list = null;
	if (from < 0 || until <= from) {
	    throw new HttpArchiveException(316,
		    "until and from not sensible. choose a valid range, please.");
	} else if (type == null || type.isEmpty() && namespace != null
		&& !namespace.isEmpty()) {
	    return listRepoNamespace(namespace, from, until);
	} else if (namespace == null || namespace.isEmpty() && type != null
		&& !type.isEmpty()) {
	    return listRepoType(type, from, until);
	} else if ((namespace == null || namespace.isEmpty())
		&& (type == null || type.isEmpty())) {
	    list = listRepoAll();
	} else {
	    list = listRepo(type, namespace);
	}
	return sublist(list, from, until);
    }

    private List<String> listRepo(String type, String namespace) {
	List<String> result = new ArrayList<String>();
	List<String> typedList = listRepoType(type);
	if (namespace != null && !namespace.isEmpty()) {
	    for (String item : typedList) {
		if (item.startsWith(namespace + ":")) {
		    result.add(item);
		}
	    }
	    return result;
	} else {
	    return typedList;
	}
    }

    private List<String> listRepoType(String type) {
	List<String> typedList;
	String query = "* <" + REL_CONTENT_TYPE + "> \"" + type + "\"";
	InputStream in = fedora.findTriples(query, FedoraVocabulary.SPO,
		FedoraVocabulary.N3);
	typedList = RdfUtils.getFedoraSubject(in);
	return typedList;
    }

    private List<String> listRepoAll() {
	List<String> typedList;
	String query = "* <" + REL_IS_NODE_TYPE + "> <" + TYPE_OBJECT + ">";
	InputStream in = fedora.findTriples(query, FedoraVocabulary.SPO,
		FedoraVocabulary.N3);
	typedList = RdfUtils.getFedoraSubject(in);
	return typedList;
    }

    private List<String> listRepoType(String type, int from, int until) {
	List<String> list = listRepoType(type);
	return sublist(list, from, until);
    }

    /**
     * List all pids within a namespace
     * 
     * @param namespace
     *            a valid namespace
     * @return a list of pids
     */
    public List<String> listRepoNamespace(String namespace) {
	return listByQuery(namespace + ":*");
    }

    private List<String> listRepoNamespace(String namespace, int from, int until) {
	List<String> list = listRepoNamespace(namespace);
	return sublist(list, from, until);
    }

    private List<String> listByQuery(String query) {
	List<String> objects = null;
	objects = fedora.findNodes(query);
	return objects;
    }

    private List<String> sublist(List<String> list, int from, int until) {
	if (from >= list.size()) {
	    return new Vector<String>();
	}
	if (until < list.size()) {
	    return list.subList(from, until);
	} else {
	    return list.subList(from, list.size());
	}
    }

    /**
     * @param type
     *            the type to be displaye
     * @param namespace
     *            list only objects in this namespace
     * @param from
     *            show only hits starting at this index
     * @param until
     *            show only hits ending at this index
     * @param getListingFrom
     *            List Resources from elasticsearch or from fedora. Allowed
     *            values: "repo" and "es"
     * @return html listing of all objects
     */
    public String listAsHtml(String type, String namespace, int from,
	    int until, String getListingFrom) {
	List<String> list = list(type, namespace, from, until, getListingFrom);
	return representations.getAllOfTypeAsHtml(list, type, namespace, from,
		until, getListingFrom);
    }

    /**
     * @param pid
     *            the pid to read from
     * @return the parentPid and contentType as json
     */
    public RegalObject getRegalObject(String pid) {
	return representations.getRegalObject(pid);
    }

    /**
     * @param pid
     *            the will be read to the node
     * @return a Node containing the data from the repository
     */
    public Node readNode(String pid) {
	return fedora.readNode(pid);
    }

    /**
     * @return an instance of services
     */
    public Services getServices() {
	return services;
    }

    /**
     * Generates a urn
     * 
     * @param pid
     *            usually the pid of an object
     * @param namespace
     *            usually the namespace
     * @param snid
     *            the urn subnamespace id
     * @return the urn
     */
    public String replaceUrn(String pid, String namespace, String snid) {
	String subject = namespace + ":" + pid;
	String urn = services.generateUrn(subject, snid);
	// String hasUrnOld =
	// "http://geni-orca.renci.org/owl/topology.owl#hasURN";
	String hasUrn = "http://purl.org/lobid/lv#urn";
	// String sameAs = "http://www.w3.org/2002/07/owl#sameAs";
	String metadata = readMetadata(subject);
	metadata = RdfUtils.replaceTriple(subject, hasUrn, urn, true, metadata);
	updateMetadata(namespace + ":" + pid, metadata);
	return "Update " + subject + " metadata " + metadata;
    }

    /**
     * Generates a urn
     * 
     * @param pid
     *            usually the pid of an object
     * @param namespace
     *            usually the namespace
     * @param snid
     *            the urn subnamespace id
     * @return the urn
     */
    public String addUrn(String pid, String namespace, String snid) {
	String subject = namespace + ":" + pid;
	String urn = services.generateUrn(subject, snid);
	// String hasUrnOld =
	// "http://geni-orca.renci.org/owl/topology.owl#hasURN";
	String hasUrn = "http://purl.org/lobid/lv#urn";
	String metadata = null;
	if (fedora.dataStreamExists(subject, "metadata")) {
	    metadata = readMetadata(subject);
	    if (RdfUtils.hasTriple(subject, hasUrn, urn, metadata))
		throw new ArchiveException(subject + "already has a urn: "
			+ metadata);
	}
	metadata = RdfUtils.addTriple(subject, hasUrn, urn, true, metadata);
	updateMetadata(namespace + ":" + pid, metadata);
	return "Update " + subject + " metadata " + metadata;
    }

    /**
     * @return the host to where the urns must point
     */
    public String getUrnbase() {
	return urnbase;
    }

    /**
     * @param p
     *            the id part of a pid
     * @param namespace
     *            the namespace part of a pid
     * @param transformerId
     *            the id of the transformer
     */
    public void addTransformer(String p, String namespace, String transformerId) {
	String pid = namespace + ":" + p;
	Node node = readNode(pid);
	node.addTransformer(new Transformer(transformerId));
	fedora.updateNode(node);
    }

    /**
     * @param node
     *            pid with namespace:pid
     * @return a aleph mab xml representation
     */
    public String aleph(Node node) {
	AlephMabMaker am = new AlephMabMaker();
	return am.aleph(node, server);
    }

    /**
     * @param pid
     *            pid with namespace:pid
     * @return a aleph mab xml representation
     */
    public String aleph(String pid) {
	return aleph(readNode(pid));
    }

    /**
     * @param pid
     *            pid with namespace:pid
     * @return a URL to a pdfa conversion
     */
    public String getPdfaUrl(String pid) {
	return getPdfaUrl(readNode(pid));
    }

    /**
     * @param node
     *            a node with a pdf data stream
     * @return a URL to a PDF/A Conversion
     */
    public String getPdfaUrl(Node node) {
	return services.getPdfaUrl(node, fedoraExtern);
    }

    /**
     * @param pid
     *            the pid of the object
     * @return a epicur display for the pid
     */
    public String epicur(String pid) {
	String url = urnbase + pid;
	return services.epicur(url, getUrn(pid));
    }

    /**
     * @param pid
     *            The pid of an object
     * @return The metadata a oaidc-xml
     */
    public String oaidc(String pid) {
	return services.oaidc(pid);
    }

    /**
     * @param pid
     *            the pid of a node with pdf data
     * @return the plain text content of the pdf
     */
    public String pdfbox(String pid) {
	return services.pdfbox(readNode(pid), fedoraExtern);
    }

    /**
     * @param node
     *            the node with pdf data
     * @return the plain text content of the pdf
     */
    public String pdfbox(Node node) {
	return services.pdfbox(node, fedoraExtern);
    }

    /**
     * @param node
     *            the node with pdf data
     * @return the plain text content of the pdf
     */
    public String itext(Node node) {
	return services.itext(node, fedoraExtern);
    }

    /**
     * @param pid
     *            the pid
     * @param format
     *            application/rdf+xml text/plain application/json
     * @return a oai_ore resource map
     */
    public String oaiore(String pid, String format) {
	List<String> parents = getRelatives(pid, IS_PART_OF);
	List<String> children = getRelatives(pid, HAS_PART);
	return representations.getReM(pid, format, fedoraExtern, parents,
		children);
    }
}
