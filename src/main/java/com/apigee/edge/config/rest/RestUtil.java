/**
 * Copyright (C) 2016 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apigee.edge.config.rest;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apigee.edge.config.utils.PrintUtil;
import com.apigee.edge.config.utils.ServerProfile;
import com.apigee.mgmtapi.sdk.client.MgmtAPIClient;
import com.apigee.mgmtapi.sdk.model.AccessToken;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.MultipartContent;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

public class RestUtil {

	static String versionRevision;
	static Logger logger = LoggerFactory.getLogger(RestUtil.class);
	static String accessToken = null;
	
	/**
	 * the factory method used by the google http client to setup JSON parsing
	 */
	private JsonFactory JSON_FACTORY = new GsonFactory();

	/**
	 * HTTP request factory used to construct the http requests
	 */
	private static HttpRequestFactory REQUEST_FACTORY;
	private static HttpRequestFactory APACHE_REQUEST_FACTORY;

	private ServerProfile profile;
	
	private static String MULTIPART_BOUNDARY_PREFIX = "----ApigeeKeystoreBoundary";

	public RestUtil(ServerProfile profile) {
		this.profile = profile;

		HttpTransport httpTransport;
		ApacheHttpTransport apacheHttpTransport;

		if (profile.getApacheHttpClient() != null) {
			httpTransport = new ApacheHttpTransport(profile.getApacheHttpClient());
			apacheHttpTransport = new ApacheHttpTransport(profile.getApacheHttpClient());
		} else {
			httpTransport = new NetHttpTransport();
			apacheHttpTransport = new ApacheHttpTransport();
		}

		REQUEST_FACTORY = httpTransport.createRequestFactory(new HttpRequestInitializer() {
			// @Override
			public void initialize(HttpRequest request) {
				request.setParser(JSON_FACTORY.createJsonObjectParser());
				XTrustProvider.install();
				// FIXME this is bad - Install the all-trusting host name verifier
				HttpsURLConnection.setDefaultHostnameVerifier(new FakeHostnameVerifier());
			}
		});

		APACHE_REQUEST_FACTORY = apacheHttpTransport.createRequestFactory(new HttpRequestInitializer() {
			// @Override
			public void initialize(HttpRequest request) {
				request.setParser(JSON_FACTORY.createJsonObjectParser());
				XTrustProvider.install();
				// FIXME this is bad - Install the all-trusting host name verifier
				HttpsURLConnection.setDefaultHostnameVerifier(new FakeHostnameVerifier());
			}
		});

	}

	/*
	 * static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport(); static
	 * final HttpTransport APACHE_HTTP_TRANSPORT = new ApacheHttpTransport(); static
	 * final JsonFactory JSON_FACTORY = new JacksonFactory();
	 * 
	 * 
	 * static HttpRequestFactory REQUEST_FACTORY = HTTP_TRANSPORT
	 * .createRequestFactory(new HttpRequestInitializer() { // @Override public void
	 * initialize(HttpRequest request) {
	 * request.setParser(JSON_FACTORY.createJsonObjectParser());
	 * XTrustProvider.install(); FakeHostnameVerifier _hostnameVerifier = new
	 * FakeHostnameVerifier(); // Install the all-trusting host name verifier:
	 * HttpsURLConnection.setDefaultHostnameVerifier(_hostnameVerifier);
	 * 
	 * } });
	 * 
	 * static HttpRequestFactory APACHE_REQUEST_FACTORY = APACHE_HTTP_TRANSPORT
	 * .createRequestFactory(new HttpRequestInitializer() { // @Override public void
	 * initialize(HttpRequest request) {
	 * request.setParser(JSON_FACTORY.createJsonObjectParser());
	 * XTrustProvider.install(); FakeHostnameVerifier _hostnameVerifier = new
	 * FakeHostnameVerifier(); // Install the all-trusting host name verifier:
	 * HttpsURLConnection.setDefaultHostnameVerifier(_hostnameVerifier);
	 * 
	 * } });
	 */

	/***************************************************************************
	 * Env Config - get, create, update
	 **/
	public HttpResponse createEnvConfig(ServerProfile profile, String resource, String payload) throws IOException {

		ByteArrayContent content = new ByteArrayContent("application/json", payload.getBytes());

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/environments/" + profile.getEnvironment() + "/" + resource;

		HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse createEnvConfig(ServerProfile profile, String resource, String resourceId, String subResource,
			String payload) throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/environments/" + profile.getEnvironment() + "/" + resource + "/"
				+ URLEncoder.encode(resourceId, "UTF-8") + "/" + URLEncoder.encode(subResource, "UTF-8");

		return executeAPIPost(profile, payload, importCmd);
	}
	
	public HttpResponse createEnvConfigWithParameters(ServerProfile profile, String resource, String resourceId, String subResource, Map<String, String> parameters,
			String payload) throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/environments/" + profile.getEnvironment() + "/" + resource + "/"
				+ URLEncoder.encode(resourceId, "UTF-8") + "/" + URLEncoder.encode(subResource, "UTF-8");

		ByteArrayContent content = new ByteArrayContent("application/json", payload.getBytes());
		
		GenericUrl url = new GenericUrl(importCmd);
		url.putAll(parameters);

		HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(url, content);
		restRequest.setReadTimeout(0);

		HttpResponse response;
		try {
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse createEnvConfigUpload(ServerProfile profile, String resource, String filePath)
			throws IOException {
		byte[] file = Files.readAllBytes(new File(filePath).toPath());
		ByteArrayContent content = new ByteArrayContent("application/octet-stream", file);

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/environments/" + profile.getEnvironment() + "/" + resource;

		HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	//Mainly used for Creating alias (for keystore)
	public HttpResponse createEnvConfigUpload(ServerProfile profile, String resource, String resourceId, String subResource, 
			Map<String, String> multipartFiles, Map<String, String> parameters) throws IOException {
		MultipartContent payload = new MultipartContent().setMediaType(new HttpMediaType("multipart/form-data"));
		payload.setBoundary(MULTIPART_BOUNDARY_PREFIX + System.currentTimeMillis());
		
		if (multipartFiles.entrySet().size() > 0) {
    		for (Map.Entry<String, String> entry : multipartFiles.entrySet()) {
    			byte[] file = Files.readAllBytes(new File(entry.getValue()).toPath());
    			ByteArrayContent content = new ByteArrayContent("application/octet-stream", file);

    			HttpHeaders headers = new HttpHeaders().set("Content-Disposition",
    					"form-data; name=\"" + entry.getKey() + "\"");

    			payload.addPart(new MultipartContent.Part(headers, content));
    		} 
    	}
		
		if(parameters!=null && parameters.containsKey("password")) {
			HttpHeaders headers = new HttpHeaders().set("Content-Disposition",
					"form-data; name=\"password\"");
			HttpContent partContent = ByteArrayContent.fromString(null, parameters.get("password"));
			payload.addPart(new MultipartContent.Part(headers, partContent));
			parameters.remove("password");
		}
		
		String importCmd = profile.getHostUrl() + "/"
    			+ profile.getApi_version() + "/organizations/"
    			+ profile.getOrg() + "/environments/"
    			+ profile.getEnvironment() + "/" + resource + "/"
    			+ URLEncoder.encode(resourceId, "UTF-8")
    			+ "/" + subResource;
		GenericUrl url = new GenericUrl(importCmd);
		url.putAll(parameters);
		HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(url, payload);
    	restRequest.setReadTimeout(0);
    	HttpResponse response;
		try {
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}
	
	public HttpResponse updateEnvConfigUpload(ServerProfile profile, String resource, String resourceId, String subResource, String subResourceId,
			Map<String, String> multipartFiles, Map<String, String> parameters) throws IOException {
		MultipartContent payload = new MultipartContent().setMediaType(new HttpMediaType("multipart/form-data"));
		payload.setBoundary(MULTIPART_BOUNDARY_PREFIX + System.currentTimeMillis());
		
		if (multipartFiles.entrySet().size() > 0) {
    		for (Map.Entry<String, String> entry : multipartFiles.entrySet()) {
    			byte[] file = Files.readAllBytes(new File(entry.getValue()).toPath());
    			ByteArrayContent content = new ByteArrayContent("application/octet-stream", file);

    			HttpHeaders headers = new HttpHeaders().set("Content-Disposition",
    					"form-data; name=\"" + entry.getKey() + "\"");

    			payload.addPart(new MultipartContent.Part(headers, content));
    		} 
    	}
		String importCmd = profile.getHostUrl() + "/"
    			+ profile.getApi_version() + "/organizations/"
    			+ profile.getOrg() + "/environments/"
    			+ profile.getEnvironment() + "/" + resource + "/"
    			+ URLEncoder.encode(resourceId, "UTF-8")
    			+ "/" + subResource + "/"
    			+ URLEncoder.encode(subResourceId, "UTF-8");
		GenericUrl url = new GenericUrl(importCmd);
		url.putAll(parameters);
		HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(url, payload);
    	restRequest.setReadTimeout(0);
		
    	HttpResponse response;
		try {
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse updateEnvConfig(ServerProfile profile, String resource, String resourceId, String payload)
			throws IOException {

		ByteArrayContent content = new ByteArrayContent("application/json", payload.getBytes());

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/environments/" + profile.getEnvironment() + "/" + resource + "/"
				+ URLEncoder.encode(resourceId, "UTF-8");

		HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse updateEnvConfig(ServerProfile profile, String resource, String resourceId, String subResource,
			String subResourceId, String payload) throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/environments/" + profile.getEnvironment() + "/" + resource + "/"
				+ URLEncoder.encode(resourceId, "UTF-8") + "/" + URLEncoder.encode(subResource, "UTF-8") + "/"
				+ URLEncoder.encode(subResourceId, "UTF-8");

		return executeAPIPost(profile, payload, importCmd);
	}

	public HttpResponse updateEnvConfigUpload(ServerProfile profile, String resource, String resourceId,
			String filePath) throws IOException {

		byte[] file = Files.readAllBytes(new File(filePath).toPath());
		ByteArrayContent content = new ByteArrayContent("application/octet-stream", file);

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/environments/" + profile.getEnvironment() + "/" + resource + "/" + resourceId;

		HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse deleteEnvResourceFileConfig(ServerProfile profile, String resource, String resourceId)
			throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/environments/" + profile.getEnvironment() + "/" + resource + "/" + resourceId;

		HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(new GenericUrl(importCmd));
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse deleteEnvConfig(ServerProfile profile, String resource, String resourceId) throws IOException {
		return deleteEnvConfig(profile, resource, resourceId, null);
	}

	public HttpResponse deleteEnvConfig(ServerProfile profile, String resource, String resourceId, String payload)
			throws IOException {
		HttpRequest restRequest;
		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/environments/" + profile.getEnvironment() + "/" + resource + "/"
				+ URLEncoder.encode(resourceId, "UTF-8");

		if (payload != null && !payload.equalsIgnoreCase("")) {
			ByteArrayContent content = new ByteArrayContent("application/json", payload.getBytes());
			restRequest = REQUEST_FACTORY.buildRequest(HttpMethods.DELETE, new GenericUrl(importCmd), content);
		} else {
			restRequest = REQUEST_FACTORY.buildDeleteRequest(new GenericUrl(importCmd));
		}
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse getEnvConfig(ServerProfile profile, String resource) throws IOException {

		HttpRequest restRequest = REQUEST_FACTORY.buildGetRequest(
				new GenericUrl(profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/"
						+ profile.getOrg() + "/environments/" + profile.getEnvironment() + "/" + resource));
		restRequest.setReadTimeout(0);

		// logger.debug(PrintUtil.formatRequest(restRequest));

		HttpResponse response = null;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			if (e.getStatusCode() == 404)
				return null;
			logger.error(e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse getEnvConfig(ServerProfile profile, String resource, String resourceId, String subResource,
			String subResourceId) throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/environments/" + profile.getEnvironment() + "/" + resource + "/"
				+ URLEncoder.encode(resourceId, "UTF-8") + "/" + URLEncoder.encode(subResource, "UTF-8") + "/"
				+ URLEncoder.encode(subResourceId, "UTF-8");

		return executeAPIGet(profile, importCmd);
	}

	public HttpResponse patchEnvConfig(ServerProfile profile, String resource, String resourceId, String payload)
			throws IOException {

		ByteArrayContent content = new ByteArrayContent("application/json", payload.getBytes());

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/environments/" + profile.getEnvironment() + "/" + resource + "/"
				+ URLEncoder.encode(resourceId, "UTF-8");

		HttpRequest restRequest = APACHE_REQUEST_FACTORY.buildRequest(HttpMethods.PATCH, new GenericUrl(importCmd),
				content);
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	/***************************************************************************
	 * Org Config - get, create, update
	 **/
	public HttpResponse createOrgConfig(ServerProfile profile, String resource, String payload) throws IOException {

		ByteArrayContent content = new ByteArrayContent("application/json", payload.getBytes());

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/" + resource;

		HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse createOrgConfig(ServerProfile profile, String resource, String resourceId, String subResource,
			String payload) throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/" + resource + "/" + URLEncoder.encode(resourceId, "UTF-8") + "/"
				+ URLEncoder.encode(subResource, "UTF-8");

		return executeAPIPost(profile, payload, importCmd);
	}

	public HttpResponse createOrgConfigUpload(ServerProfile profile, String resource, String filePath)
			throws IOException {
		byte[] file = Files.readAllBytes(new File(filePath).toPath());
		ByteArrayContent content = new ByteArrayContent("application/octet-stream", file);

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/" + resource;

		HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse updateOrgConfig(ServerProfile profile, String resource, String resourceId, String payload)
			throws IOException {

		ByteArrayContent content = new ByteArrayContent("application/json", payload.getBytes());

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/" + resource + "/" + URLEncoder.encode(resourceId, "UTF-8");

		HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse updateOrgConfig(ServerProfile profile, String resource, String resourceId, String subResource,
			String subResourceId, String payload) throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/" + resource + "/" + URLEncoder.encode(resourceId, "UTF-8") + "/"
				+ URLEncoder.encode(subResource, "UTF-8") + "/" + URLEncoder.encode(subResourceId, "UTF-8");

		return executeAPIPost(profile, payload, importCmd);
	}

	public HttpResponse updateOrgConfigUpload(ServerProfile profile, String resource, String resourceId,
			String filePath) throws IOException {

		byte[] file = Files.readAllBytes(new File(filePath).toPath());
		ByteArrayContent content = new ByteArrayContent("application/octet-stream", file);

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/" + resource + "/" + resourceId;

		HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse deleteOrgConfig(ServerProfile profile, String resource, String resourceId) throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/" + resource + "/" + URLEncoder.encode(resourceId, "UTF-8");

		HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(new GenericUrl(importCmd));
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse deleteOrgResourceFileConfig(ServerProfile profile, String resource, String resourceId)
			throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/" + resource + "/" + resourceId;

		HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(new GenericUrl(importCmd));
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse getOrgConfig(ServerProfile profile, String resource) throws IOException {

		HttpRequest restRequest = REQUEST_FACTORY.buildGetRequest(new GenericUrl(profile.getHostUrl() + "/"
				+ profile.getApi_version() + "/organizations/" + profile.getOrg() + "/" + resource));
		restRequest.setReadTimeout(0);

		// logger.debug(PrintUtil.formatRequest(restRequest));

		HttpResponse response = null;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			if (e.getStatusCode() == 404)
				return null;
			logger.error(e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse getOrgConfig(ServerProfile profile, String resource, String resourceId, String subResource,
			String subResourceId) throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/" + resource + "/" + URLEncoder.encode(resourceId, "UTF-8") + "/"
				+ URLEncoder.encode(subResource, "UTF-8") + "/" + URLEncoder.encode(subResourceId, "UTF-8");

		return executeAPIGet(profile, importCmd);
	}

	/***************************************************************************
	 * API Config - get, create, update
	 **/
	public HttpResponse createAPIConfig(ServerProfile profile, String api, String resource, String payload)
			throws IOException {

		ByteArrayContent content = new ByteArrayContent("application/json", payload.getBytes());

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/apis/" + api + "/" + resource;

		HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse createAPIConfig(ServerProfile profile, String api, String resource, String resourceId,
			String subResource, String payload) throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/apis/" + api + "/" + resource + "/" + URLEncoder.encode(resourceId, "UTF-8") + "/"
				+ URLEncoder.encode(subResource, "UTF-8");

		return executeAPIPost(profile, payload, importCmd);
	}

	public HttpResponse createAPIConfigUpload(ServerProfile profile, String api, String resource, String filePath)
			throws IOException {
		byte[] file = Files.readAllBytes(new File(filePath).toPath());
		ByteArrayContent content = new ByteArrayContent("application/octet-stream", file);

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/apis/" + api + "/" + resource;

		HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse updateAPIConfig(ServerProfile profile, String api, String resource, String resourceId,
			String payload) throws IOException {

		ByteArrayContent content = new ByteArrayContent("application/json", payload.getBytes());

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/apis/" + api + "/" + resource + "/" + URLEncoder.encode(resourceId, "UTF-8");

		HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse updateAPIConfig(ServerProfile profile, String api, String resource, String resourceId,
			String subResource, String subResourceId, String payload) throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/apis/" + api + "/" + resource + "/" + URLEncoder.encode(resourceId, "UTF-8") + "/"
				+ URLEncoder.encode(subResource, "UTF-8") + "/" + URLEncoder.encode(subResourceId, "UTF-8");

		return executeAPIPost(profile, payload, importCmd);
	}

	public HttpResponse updateAPIConfigUpload(ServerProfile profile, String api, String resource, String resourceId,
			String filePath) throws IOException {

		byte[] file = Files.readAllBytes(new File(filePath).toPath());
		ByteArrayContent content = new ByteArrayContent("application/octet-stream", file);

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/apis/" + api + "/" + resource + "/" + resourceId;

		HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse deleteAPIConfig(ServerProfile profile, String api, String resource, String resourceId)
			throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/apis/" + api + "/" + resource + "/" + URLEncoder.encode(resourceId, "UTF-8");

		HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(new GenericUrl(importCmd));
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse getAPIConfig(ServerProfile profile, String api, String resource) throws IOException {

		HttpRequest restRequest = REQUEST_FACTORY.buildGetRequest(new GenericUrl(profile.getHostUrl() + "/"
				+ profile.getApi_version() + "/organizations/" + profile.getOrg() + "/apis/" + api + "/" + resource));
		restRequest.setReadTimeout(0);

		// logger.debug(PrintUtil.formatRequest(restRequest));

		HttpResponse response = null;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			if (e.getStatusCode() == 404)
				return null;
			logger.error(e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse getAPIConfig(ServerProfile profile, String api, String resource, String resourceId,
			String subResource, String subResourceId) throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/apis/" + api + "/" + resource + "/" + URLEncoder.encode(resourceId, "UTF-8") + "/"
				+ URLEncoder.encode(subResource, "UTF-8") + "/" + URLEncoder.encode(subResourceId, "UTF-8");

		return executeAPIGet(profile, importCmd);
	}

	public HttpResponse deleteAPIResourceFileConfig(ServerProfile profile, String api, String resource,
			String resourceId) throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/apis/" + api + "/" + resource + "/" + resourceId;

		HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(new GenericUrl(importCmd));
		restRequest.setReadTimeout(0);

		// logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			// response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public void initMfa(ServerProfile profile) throws IOException {

		// any simple get request can be used to - we just need to get an access token
		// whilst the mfatoken is still valid

		// trying to construct the URL like
		// https://api.enterprise.apigee.com/v1/organizations/apigee-cs/apis/
		// success response is ignored
		if (accessToken == null) {
			logger.info("=============Initialising MFA================");

			HttpRequest restRequest = REQUEST_FACTORY.buildGetRequest(new GenericUrl(profile.getHostUrl() + "/"
					+ profile.getApi_version() + "/organizations/" + profile.getOrg() + "/apis/"));
			restRequest.setReadTimeout(0);

			try {
				HttpResponse response = executeAPI(profile, restRequest);
				// ignore response - we just wanted the MFA initialised
				logger.info("=============MFA Initialised================");
			} catch (HttpResponseException e) {
				logger.error(e.getMessage());
				// throw error as there is no point in continuing
				throw e;
			}
		}
	}

	private HttpResponse executeAPIGet(ServerProfile profile, String importCmd) throws IOException {

		HttpRequest restRequest = REQUEST_FACTORY.buildGetRequest(new GenericUrl(importCmd));
		restRequest.setReadTimeout(0);

		HttpResponse response;
		try {
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			if (e.getStatusCode() == 404)
				return null;
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	private HttpResponse executeAPIPost(ServerProfile profile, String payload, String importCmd) throws IOException {

		ByteArrayContent content = new ByteArrayContent("application/json", payload.getBytes());

		HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		HttpResponse response;
		try {
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	/**
	 * OAuth token acquisition for calling management APIs Access Token expiry 1799
	 * sec = 30 mins long enough to finish any maven task MFA Token: TOTP expires in
	 * 30 secs. User needs to give a token with some validity
	 */
	private HttpResponse executeAPI(ServerProfile profile, HttpRequest request) throws IOException {
		HttpHeaders headers = request.getHeaders();
		// MgmtAPIClient client = new MgmtAPIClient();
		MgmtAPIClient client = new MgmtAPIClient(profile);
		String mfaToken = profile.getMFAToken();
		String tokenUrl = profile.getTokenUrl();
		String mgmtAPIClientId = (profile.getClientId() != null && !profile.getClientId().equalsIgnoreCase(""))
				? profile.getClientId()
				: "edgecli";
		String mgmtAPIClientSecret = (profile.getClientSecret() != null
				&& !profile.getClientSecret().equalsIgnoreCase("")) ? profile.getClientSecret() : "edgeclisecret";
		headers.setAccept("application/json");
		/**** Basic Auth - Backward compatibility ****/
		if (profile.getAuthType() != null && profile.getAuthType().equalsIgnoreCase("basic")) {
			headers.setBasicAuthentication(profile.getCredential_user(), profile.getCredential_pwd());
			logger.info(PrintUtil.formatRequest(request));
			return request.execute();
		}
		/**** OAuth ****/
		if (profile.getBearerToken() != null && !profile.getBearerToken().equalsIgnoreCase("")) {
			// Need to validate access token only if refresh token is provided.
			// If access token is not valid, create a bearer token using the refresh token
			// If access token is valid, use that
			accessToken = (accessToken != null) ? accessToken : profile.getBearerToken();
			if (profile.getRefreshToken() != null && !profile.getRefreshToken().equalsIgnoreCase("")) {
				if (isValidBearerToken(accessToken, profile, mgmtAPIClientId)) {
					logger.info("Access Token valid");
					headers.setAuthorization("Bearer " + accessToken);
				} else {
					try {
						AccessToken token = null;
						logger.info("Access token not valid so acquiring new access token using Refresh Token");
						token = client.getAccessTokenFromRefreshToken(tokenUrl, mgmtAPIClientId, mgmtAPIClientSecret,
								profile.getRefreshToken());
						logger.info("New Access Token acquired");
						accessToken = token.getAccess_token();
						headers.setAuthorization("Bearer " + accessToken);
					} catch (Exception e) {
						logger.error(e.getMessage());
						throw new IOException(e.getMessage());
					}
				}
			}
			// if refresh token is not passed, validate the access token and use it
			// accordingly
			else {
				logger.info("Validating the access token passed");
				if (isValidBearerToken(profile.getBearerToken(), profile, mgmtAPIClientId)) {
					logger.info("Access Token valid");
					accessToken = profile.getBearerToken();
					headers.setAuthorization("Bearer " + accessToken);
				} else {
					logger.error("Access token not valid");
					throw new IOException("Access token not valid");
				}

			}
		} else if (accessToken != null) {
			// subsequent calls
			logger.debug("Reusing mgmt API access token");
			headers.setAuthorization("Bearer " + accessToken);
		} else {
			logger.info("Acquiring mgmt API token from " + tokenUrl);
			try {
				AccessToken token = null;
				if (mfaToken == null || mfaToken.length() == 0) {
					logger.info("MFA token not provided. Skipping.");
					token = client.getAccessToken(tokenUrl, mgmtAPIClientId, mgmtAPIClientSecret,
							profile.getCredential_user(), profile.getCredential_pwd());
				} else {
					logger.info("Making use of the MFA token provided.");
					token = client.getAccessToken(tokenUrl, mgmtAPIClientId, mgmtAPIClientSecret,
							profile.getCredential_user(), profile.getCredential_pwd(), profile.getMFAToken());
				}
				accessToken = token.getAccess_token();
				headers.setAuthorization("Bearer " + accessToken);
			} catch (Exception e) {
				logger.error(e.getMessage());
				throw new IOException(e.getMessage());
			}
		}
		logger.info(PrintUtil.formatRequest(request));
		return request.execute();
	}

	/**
	 * This method is used to validate the Bearer token. It validates the source and
	 * the expiration and if the token is about to expire in 30 seconds, set as
	 * invalid token
	 * 
	 * @param accessToken
	 * @param profile
	 * @param clientId
	 * @return
	 * @throws IOException
	 */
	private boolean isValidBearerToken(String accessToken, ServerProfile profile, String clientId) throws IOException {
		boolean isValid = false;
		try {
			JWT jwt = JWT.decode(accessToken);
			String jwtClientId = jwt.getClaim("client_id").asString();
			String jwtEmailId = jwt.getClaim("email").asString();
			long jwtExpiresAt = jwt.getExpiresAt().getTime() / 1000;
			long difference = jwtExpiresAt - (System.currentTimeMillis() / 1000);
			if (jwt != null && jwtClientId != null && jwtClientId.equals(clientId) && jwtEmailId != null
					&& jwtEmailId.equalsIgnoreCase(profile.getCredential_user())
					&& profile.getTokenUrl().contains(jwt.getIssuer()) && difference >= 30) {
				isValid = true;
			}
		} catch (JWTDecodeException exception) {
			throw new IOException(exception.getMessage());
		}
		return isValid;
	}

	/*
	 * API Spec
	 */
	public HttpResponse getAllAPISpecs(ServerProfile profile) throws IOException {

		GenericUrl url = new GenericUrl(
				format("%s/organizations/%s/specs/folder/home", profile.getHostUrl(), profile.getOrg()));
		HttpRequest restRequest = REQUEST_FACTORY.buildGetRequest(url);
		restRequest.setReadTimeout(0);
		HttpResponse response = null;
		try {
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			if (e.getStatusCode() == 404)
				return null;
			logger.error(e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

	public HttpResponse createAPISpec(ServerProfile profile, String payload) throws IOException {
		ByteArrayContent content = new ByteArrayContent("application/json", payload.getBytes());
		GenericUrl url = new GenericUrl(
				format("%s/organizations/%s/specs/doc", profile.getHostUrl(), profile.getOrg()));
		HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(url, content);
		restRequest.setReadTimeout(0);
		HttpResponse response = null;
		try {
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			if (e.getStatusCode() == 404)
				return null;
			logger.error(e.getMessage());
			throw new IOException(e.getMessage());
		}
		return response;
	}

	public HttpResponse uploadKeycertfile(ServerProfile profile, String aliasName, String keystore, String certFilePath,
			String keyFilePath, String password) throws IOException {
		HttpResponse response = null;
		byte[] certFile = null;
		byte[] keyFile = null;
		certFile = Files.readAllBytes(new File(certFilePath).toPath());
		if (keyFilePath != null && !keyFilePath.equals("")) // will be null if key file is not sent
			keyFile = Files.readAllBytes(new File(keyFilePath).toPath());

		return response;
	}

	public HttpResponse uploadAPISpec(ServerProfile profile, String id, String filePath) throws IOException {
		byte[] file = Files.readAllBytes(new File(filePath).toPath());
		ByteArrayContent content = new ByteArrayContent("text/plain", file);

		GenericUrl url = new GenericUrl(
				format("%s/organizations/%s/specs/doc/%s/content", profile.getHostUrl(), profile.getOrg(), id));
		HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(url, content);
		restRequest.setReadTimeout(0);
		HttpResponse response = null;
		try {
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			if (e.getStatusCode() == 404)
				return null;
			logger.error(e.getMessage());
			throw new IOException(e.getMessage());
		}
		return response;
	}

	public HttpResponse deleteAPISpec(ServerProfile profile, String id) throws IOException {

		GenericUrl url = new GenericUrl(
				format("%s/organizations/%s/specs/doc/%s", profile.getHostUrl(), profile.getOrg(), id));
		HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(url);
		restRequest.setReadTimeout(0);
		HttpResponse response;
		try {
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}
		return response;
	}

	public ServerProfile getProfile() {
		return profile;
	}
}
