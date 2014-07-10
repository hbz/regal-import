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
package controllers;

import static de.nrw.hbz.regal.fedora.FedoraVocabulary.HAS_PART;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.IS_PART_OF;
import helper.Actions;
import helper.HttpArchiveException;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import models.DCBeanAnnotated;
import models.Message;
import models.ObjectList;
import models.RegalObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import actions.BasicAuth;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;

import de.nrw.hbz.regal.datatypes.Node;
import de.nrw.hbz.regal.exceptions.ArchiveException;

/**
 * 
 * @author Jan Schnasse, schnasse@hbz-nrw.de
 * 
 *         Api is documented using swagger. See:
 *         https://github.com/wordnik/swagger-ui
 * 
 */
@BasicAuth
@Api(value = "/resource", description = "The resource endpoint allows one to manipulate and access complex objects as http resources. ")
@SuppressWarnings("javadoc")
public class Resource extends MyController {

    final static Logger logger = LoggerFactory.getLogger(Resource.class);

    @ApiOperation(produces = "application/json,text/html", nickname = "listResources", value = "listResources", notes = "Returns a list of ids", response = ObjectList.class, httpMethod = "GET")
    public static Result listResources(
	    @QueryParam("namespace") String namespace,
	    @QueryParam("contentType") String contentType,
	    @QueryParam("src") String src, @QueryParam("from") int from,
	    @QueryParam("until") int until) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    if (request().accepts("text/html")) {
		response().setHeader("Access-Control-Allow-Origin", "*");
		response().setContentType("text/html");
		Actions actions = Actions.getInstance();
		String rem = actions.listAsHtml(contentType, namespace, from,
			until, src);
		return ok(rem);
	    } else {
		response().setHeader("Access-Control-Allow-Origin", "*");
		response().setContentType("application/json");
		Actions actions = Actions.getInstance();
		ObjectList rem = new ObjectList(actions.list(contentType,
			namespace, from, until, src));
		return JsonResponse(rem);
	    }
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/json+regal-v0.4.0,application/json,text/html,application/json+compact,application/rdf+xml,text/plain", nickname = "listResource", value = "listResource", notes = "Returns a resource. Redirects in dependends to the accept header ", response = Message.class, httpMethod = "GET")
    public static Result listResource(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    response().setHeader("Access-Control-Allow-Origin", "*");
	    if (request().accepts("text/html"))
		return asHtml(pid);
	    if (request().accepts("application/json"))
		return asJson(pid, null);
	    if (request().accepts("application/json+compact"))
		return asJson(pid, "compact");
	    if (request().accepts("application/rdf+xml"))
		return asRdf(pid);
	    if (request().accepts("text/plain"))
		return asRdf(pid);
	    if (request().accepts("application/json+regal-v0.4.0"))
		return asRegalObject(pid);

	    return asRdf(pid);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (ArchiveException e) {
	    response().setContentType("application/json");
	    return JsonResponse(new Message(e, 404), 404);

	} catch (Exception e) {
	    response().setContentType("application/json");
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "text/plain", nickname = "listMetadata", value = "listMetadata", notes = "Shows Metadata of a resource.", response = play.mvc.Result.class, httpMethod = "GET")
    public static Result listMetadata(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    response().setHeader("Access-Control-Allow-Origin", "*");
	    String result = actions.readMetadata(pid);
	    return ok(result);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/octet-stream", nickname = "listData", value = "listData", notes = "Shows Data of a resource", response = play.mvc.Result.class, httpMethod = "GET")
    public static Result listData(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    response().setHeader("Access-Control-Allow-Origin", "*");
	    URL url = new URL(actions.getServer() + "/fedora/objects/" + pid
		    + "/datastreams/data/content");
	    HttpURLConnection connection = (HttpURLConnection) url
		    .openConnection();
	    InputStream is = connection.getInputStream();
	    response().setContentType("application/pdf");
	    return ok(is);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/json", nickname = "listDc", value = "listDc", notes = "Shows internal dublin core stream", response = play.mvc.Result.class, httpMethod = "GET")
    public static Result listDc(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);

	    response().setHeader("Access-Control-Allow-Origin", "*");
	    DCBeanAnnotated dc = actions.readDC(pid);
	    return JsonResponse(dc);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/json", nickname = "updateResource", value = "updateResource", notes = "Updates or Creates a Resource with the path decoded pid", response = Message.class, httpMethod = "PUT")
    @ApiImplicitParams({ @ApiImplicitParam(value = "New Object", required = true, dataType = "RegalObject", paramType = "body") })
    public static Result updateResource(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    if (!modifyingAccessIsAllowed(role))
		throw new HttpArchiveException(401);
	    Actions actions = Actions.getInstance();
	    String[] p = pid.split(":");
	    Object o = request().body().asJson();
	    RegalObject object;
	    if (o != null) {
		object = (RegalObject) MyController.mapper.readValue(
			o.toString(), RegalObject.class);
	    } else {
		object = new RegalObject();
	    }
	    Node node = actions.createResource(object.getType(),
		    object.getParentPid(), object.getTransformer(),
		    object.getAccessScheme(), p[1], p[0]);
	    String result = node.getPID() + " created/updated!";
	    return JsonResponse(new Message(result, 200));
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (NullPointerException e) {
	    return JsonResponse(new Message(e, 400), 400);
	} catch (ArchiveException e) {
	    return JsonResponse(new Message(e, 404), 404);
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/json", nickname = "updateMetadata", value = "updateMetadata", notes = "Updates the metadata of the resource using n-triples.", response = Message.class, httpMethod = "PUT")
    @ApiImplicitParams({ @ApiImplicitParam(value = "Metadata", required = true, dataType = "string", paramType = "body") })
    public static Result updateMetadata(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    if (!modifyingAccessIsAllowed(role))
		throw new HttpArchiveException(401);
	    Actions actions = Actions.getInstance();
	    String result = actions.updateMetadata(pid, request().body()
		    .asText());
	    return JsonResponse(new Message(result));
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (NullPointerException e) {
	    return JsonResponse(new Message(e, 400), 400);
	} catch (ArchiveException e) {
	    return JsonResponse(new Message(e, 404), 404);
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @SuppressWarnings("unused")
    @ApiOperation(produces = "application/json", nickname = "updateData", value = "updateData", notes = "Updates the data of a resource", response = Message.class, httpMethod = "PUT")
    @ApiImplicitParams({ @ApiImplicitParam(name = "data", value = "data", dataType = "file", required = true, paramType = "body") })
    public static Result updateData(@PathParam("pid") String pid,
	    @QueryParam("md5") String md5) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    if (!modifyingAccessIsAllowed(role))
		throw new HttpArchiveException(401);
	    Actions actions = Actions.getInstance();
	    MultipartFormData body = request().body().asMultipartFormData();
	    FilePart d = body.getFile("data");
	    String mimeType = d.getContentType();
	    String name = d.getFilename();
	    if (d == null) {
		return JsonResponse(new Message("Missing File.", 400), 400);
	    }
	    FileInputStream content = new FileInputStream(d.getFile());
	    actions.updateData(pid, content, mimeType, name, md5);
	    return JsonResponse(new Message("File uploaded! Type: " + mimeType
		    + ", Name: " + name));
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (NullPointerException e) {
	    return JsonResponse(new Message(e, 400), 400);
	} catch (ArchiveException e) {
	    return JsonResponse(new Message(e, 404), 404);
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}

    }

    @ApiOperation(produces = "application/json", nickname = "updateDc", value = "updateDc", notes = "Updates the dc data of a resource", response = Message.class, httpMethod = "PUT")
    public static Result updateDc(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    if (!modifyingAccessIsAllowed(role))
		throw new HttpArchiveException(401);
	    Actions actions = Actions.getInstance();
	    JsonNode json = request().body().asJson();
	    String result = actions.updateDC(pid, json);
	    return JsonResponse(new Message(result));
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/json", nickname = "deleteResource", value = "deleteResource", notes = "Deletes a resource", response = Message.class, httpMethod = "DELETE")
    public static Result deleteResource(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    if (!modifyingAccessIsAllowed(role))
		throw new HttpArchiveException(401);
	    Actions actions = Actions.getInstance();
	    String result = actions.delete(pid);
	    return JsonResponse(new Message(result));
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/json", nickname = "deleteMetadata", value = "deleteMetadata", notes = "Deletes a resources metadata", response = Message.class, httpMethod = "DELETE")
    public static Result deleteMetadata(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    if (!modifyingAccessIsAllowed(role))
		throw new HttpArchiveException(401);
	    Actions actions = Actions.getInstance();
	    String result = actions.deleteMetadata(pid);
	    return JsonResponse(new Message(result));
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/json", nickname = "deleteData", value = "deleteData", notes = "Deletes a resources data", response = Message.class, httpMethod = "DELETE")
    public static Result deleteData(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    if (!modifyingAccessIsAllowed(role))
		throw new HttpArchiveException(401);
	    Actions actions = Actions.getInstance();
	    String result = actions.deleteData(pid);
	    return JsonResponse(new Message(result));
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/json", nickname = "deleteDc", value = "deleteDc", notes = "Not implemented", response = Message.class, httpMethod = "DELETE")
    public static Result deleteDc(@PathParam("pid") String pid) {
	return JsonResponse(new Message("Not implemented!", 500), 500);
    }

    @ApiOperation(produces = "application/json", nickname = "deleteResources", value = "deleteResources", notes = "Deletes a set of resources", response = Message.class, httpMethod = "DELETE")
    public static Result deleteResources(@PathParam("pid") String namespace,
	    String type, String src, int from, int until) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    if (!modifyingAccessIsAllowed(role))
		throw new HttpArchiveException(401);
	    Actions actions = Actions.getInstance();
	    String result = actions.deleteAll(actions.list(type, namespace,
		    from, until, src));
	    return JsonResponse(new Message(result));
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/json", nickname = "listParts", value = "listParts", notes = "List resources linked with hasPart", response = play.mvc.Result.class, httpMethod = "GET")
    public static Result listParts(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    response().setHeader("Access-Control-Allow-Origin", "*");
	    ObjectList result = new ObjectList(actions.getRelatives(pid,
		    HAS_PART));
	    return JsonResponse(result);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/json", nickname = "listParents", value = "listParents", notes = "Shows resources linkes with isPartOf", response = play.mvc.Result.class, httpMethod = "GET")
    public static Result listParents(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    response().setHeader("Access-Control-Allow-Origin", "*");
	    ObjectList result = new ObjectList(actions.getRelatives(pid,
		    IS_PART_OF));
	    return JsonResponse(result);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/html", nickname = "asHtml", value = "asHtml", notes = "Returns a html display of the resource", response = Message.class, httpMethod = "GET")
    public static Result asHtml(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    String result = actions.oaiore(pid, "text/html");
	    response().setContentType("text/html;charset=utf-8");
	    return ok(result);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/rdf+xml,text/plain", nickname = "asRdf", value = "asRdf", notes = "Returns a rdf display of the resource", response = Message.class, httpMethod = "GET")
    public static Result asRdf(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    String result = "";
	    if (request().accepts("application/rdf+xml")) {
		result = actions.oaiore(pid, "application/rdf+xml");
		response().setContentType("application/rdf+xml");
		return ok(result);
	    } else if (request().accepts("text/plain")) {
		result = actions.oaiore(pid, "text/plain");
		response().setContentType("text/plain");
		return ok(result);
	    }
	    return JsonResponse(new Message(result));
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/json", nickname = "asJson", value = "asJson", notes = "Returns a json display of the resource", response = Message.class, httpMethod = "GET")
    public static Result asJson(@PathParam("pid") String pid, String style) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    String result = "ERROR";
	    if ("compact".equals(style))
		result = actions.oaiore(pid, "application/json+compact");
	    else
		result = actions.oaiore(pid, "application/json");
	    response().setContentType("application/json");
	    return ok(result);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/json", nickname = "asJsonCompact", value = "asJsonCompact", notes = "Returns a json compacted display of the resource", response = Message.class, httpMethod = "GET")
    public static Result asJsonCompact(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    String result = actions.oaiore(pid, "application/json+compact");
	    response().setContentType("application/json");
	    return JsonResponse(new Message(result));
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/xml", nickname = "asOaiDc", value = "asOaiDc", notes = "Returns a oai dc display of the resource", response = Message.class, httpMethod = "GET")
    public static Result asOaiDc(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    String result = actions.oaidc(pid);
	    response().setContentType("application/xml");
	    return ok(result);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/xml", nickname = "asEpicur", value = "asEpicur", notes = "Returns a epicur display of the resource", response = Message.class, httpMethod = "GET")
    public static Result asEpicur(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    String result = actions.epicur(pid);
	    response().setContentType("application/xml");
	    return ok(result);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/xml", nickname = "asAleph", value = "asAleph", notes = "Returns a aleph xml display of the resource", response = Message.class, httpMethod = "GET")
    public static Result asAleph(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    String result = actions.aleph(pid);
	    response().setContentType("application/xml");
	    return ok(result);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/json", nickname = "asRegalObject", value = "asRegalObject", notes = "The basic regal object", response = RegalObject.class, httpMethod = "GET")
    public static Result asRegalObject(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    response().setHeader("Access-Control-Allow-Origin", "*");
	    RegalObject result = actions.getRegalObject(pid);
	    response().setContentType("application/json");
	    return JsonResponse(result);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "application/pdf", nickname = "asPdfa", value = "asPdfa", notes = "Returns a pdfa conversion of a pdf datastream.", httpMethod = "GET")
    public static Result asPdfa(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    String redirectUrl = actions.getPdfaUrl(pid);
	    URL url = new URL(redirectUrl);
	    HttpURLConnection connection = (HttpURLConnection) url
		    .openConnection();
	    InputStream is = connection.getInputStream();
	    response().setContentType("application/pdf");
	    return ok(is);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    @ApiOperation(produces = "text/plain", nickname = "asPdfboxTxt", value = "asPdfboxTxt", notes = "Returns text display of a pdf datastream.", response = String.class, httpMethod = "GET")
    public static Result asPdfboxTxt(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    System.out.println(role);
	    Actions actions = Actions.getInstance();
	    String accessScheme = actions.readNode(pid).getAccessScheme();
	    if (!readAccessIsAllowed(accessScheme, role))
		throw new HttpArchiveException(401);
	    String result = actions.pdfbox(pid);
	    response().setContentType("text/plain");
	    return ok(result);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    public static Result updateOaiSets(@PathParam("pid") String pid) {
	try {
	    String role = (String) Http.Context.current().args.get("role");
	    if (!modifyingAccessIsAllowed(role))
		throw new HttpArchiveException(401);
	    Actions actions = Actions.getInstance();
	    String result = actions.makeOAISet(pid);
	    response().setContentType("text/plain");
	    return ok(result);
	} catch (HttpArchiveException e) {
	    return JsonResponse(new Message(e, e.getCode()), e.getCode());
	} catch (Exception e) {
	    return JsonResponse(new Message(e, 500), 500);
	}
    }

    public static boolean readAccessIsAllowed(String accessScheme, String role) {
	if (!"edoweb-admin".equals(role)) {
	    if ("public".equals(accessScheme)) {
		return true;
	    } else if ("lbz-wide".equals(accessScheme)) {
		if ("edoweb-editor".equals(role)
			|| "edoweb-reader".equals(role)) {
		    return true;
		}
	    } else if ("private".equals(accessScheme)) {
		if ("edoweb-editor".equals(role))
		    return true;
	    }
	} else {
	    return true;
	}
	return false;
    }

    public static boolean modifyingAccessIsAllowed(String role) {
	if ("edoweb-admin".equals(role) || "edoweb-editor".equals(role))
	    return true;
	return false;
    }
}