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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.apigee.edge.config.utils.PrintUtil;
import com.apigee.edge.config.utils.ServerProfile;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
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
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.oauth2.ServiceAccountCredentials;

public class RestUtil {
	private static HttpRequestFactory REQUEST_FACTORY;
	private static HttpRequestFactory APACHE_REQUEST_FACTORY;
	
    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static final HttpTransport APACHE_HTTP_TRANSPORT = new ApacheHttpTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();
    static String versionRevision;
    static Logger logger = LogManager.getLogger(RestUtil.class);
    
    static String accessToken = null;
    
    private static String MULTIPART_BOUNDARY_PREFIX = "----ApigeeKeystoreBoundary";
    
    private ServerProfile profile;
    
    public ServerProfile getProfile() {
		return profile;
	}
    
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
    
    /*static HttpRequestFactory REQUEST_FACTORY = HTTP_TRANSPORT
            .createRequestFactory(new HttpRequestInitializer() {
                // @Override
                public void initialize(HttpRequest request) {
                    request.setParser(JSON_FACTORY.createJsonObjectParser());
                    XTrustProvider.install();
                    FakeHostnameVerifier _hostnameVerifier = new FakeHostnameVerifier();
                    // Install the all-trusting host name verifier:
                    HttpsURLConnection.setDefaultHostnameVerifier(_hostnameVerifier);

                }
            });
    
    static HttpRequestFactory APACHE_REQUEST_FACTORY = APACHE_HTTP_TRANSPORT
            .createRequestFactory(new HttpRequestInitializer() {
                // @Override
                public void initialize(HttpRequest request) {
                    request.setParser(JSON_FACTORY.createJsonObjectParser());
                    XTrustProvider.install();
                    FakeHostnameVerifier _hostnameVerifier = new FakeHostnameVerifier();
                    // Install the all-trusting host name verifier:
                    HttpsURLConnection.setDefaultHostnameVerifier(_hostnameVerifier);

                }
            });*/

    /***************************************************************************
     * Env Config - get, create, update
     **/
    public HttpResponse createEnvConfig(ServerProfile profile, 
                                                String resource,
                                                String payload)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json", 
                                                            payload.getBytes());

        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/environments/"
                            + profile.getEnvironment() + "/" + resource;

        HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(
                new GenericUrl(importCmd), content);
        restRequest.setReadTimeout(0);

        //logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
            //response = restRequest.execute();
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public HttpResponse createEnvConfig(ServerProfile profile,
                                               String resource,
                                               String resourceId,
                                               String subResource,
                                               String payload)
            throws IOException {

        String importCmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg() + "/environments/"
                + profile.getEnvironment() + "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource;

        return executeAPIPost(profile, payload, importCmd);
    }
    
        public HttpResponse updateDeveloperStatus(ServerProfile profile,
                                               String resource,
                                               String developerId,
                                               String action)
            throws IOException {
        String cmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg() + "/developers/"
                + URLEncoder.encode(developerId, "UTF-8") + "?action=" + action;

        return executeAPIPost(profile, "", cmd, "application/octet-stream");
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
							+ "/environments/" + profile.getEnvironment()
							+ "/" + resource;

		HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		//logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			//response = restRequest.execute();
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
  	
  	//Mainly used for updating alias (for keystore)
  	public HttpResponse updateEnvConfigUpload(ServerProfile profile, String resource, String resourceId, String subResource, 
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
    
    public HttpResponse updateEnvConfig(ServerProfile profile, 
                                                String resource,
                                                String resourceId,
                                                String payload)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json", 
                                                            payload.getBytes());

        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/environments/"
                            + profile.getEnvironment() + "/" + resource + "/"
                            + URLEncoder.encode(resourceId, "UTF-8");

        HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(
                new GenericUrl(importCmd), content);
        restRequest.setReadTimeout(0);

        //logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
        	//response = restRequest.execute();
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public HttpResponse updateEnvConfig(ServerProfile profile,
                                               String resource,
                                               String resourceId,
                                               String subResource,
                                               String subResourceId,
                                               String payload)
            throws IOException {

        String cmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg() + "/environments/"
                + profile.getEnvironment() + "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource +"/"
                + subResourceId;

        return executeAPIPut(profile, payload, cmd);
    }

	public HttpResponse updateEnvConfigUpload(ServerProfile profile, String resource, String resourceId,
			String filePath) throws IOException {

		byte[] file = Files.readAllBytes(new File(filePath).toPath());
		ByteArrayContent content = new ByteArrayContent("application/octet-stream", file);

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
							+ "/environments/"+ profile.getEnvironment()
							+ "/" + resource + "/" + resourceId;

		HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		//logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			//response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}
	
	public HttpResponse updateEnvConfigWithParameters(ServerProfile profile, String resource, String resourceId, String subResource, Map<String, String> parameters,
			String payload) throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/environments/" + profile.getEnvironment() + "/" + resource + "/"
				+ URLEncoder.encode(resourceId, "UTF-8") + "/" + subResource;

		ByteArrayContent content = new ByteArrayContent("application/json", payload.getBytes());
		
		GenericUrl url = new GenericUrl(importCmd);
		url.putAll(parameters);

		HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(url, content);
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
	
	public HttpResponse deleteEnvResourceFileConfig(ServerProfile profile, String resource, String resourceId)
			throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
							+ "/environments/" + profile.getEnvironment()
							+ "/" + resource + "/" + resourceId;

		HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(new GenericUrl(importCmd));
		restRequest.setReadTimeout(0);

		//logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			//response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}
    
    public HttpResponse deleteEnvConfig(ServerProfile profile, 
									            String resource,
									            String resourceId)
	throws IOException {
    	return deleteEnvConfig(profile, resource, resourceId, null);
    }
    
    public HttpResponse deleteEnvConfig(ServerProfile profile, 
                                                String resource,
                                                String resourceId,
                                                String payload)
            throws IOException {
    	HttpRequest restRequest;
        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/environments/"
                            + profile.getEnvironment() + "/" + resource + "/"
                            + URLEncoder.encode(resourceId, "UTF-8");
        
        if(payload!=null && !payload.equalsIgnoreCase("")){
        	ByteArrayContent content = new ByteArrayContent("application/json", 
                    payload.getBytes());
        	restRequest = REQUEST_FACTORY.buildRequest(HttpMethods.DELETE, new GenericUrl(importCmd), content);
        }else{
        	restRequest = REQUEST_FACTORY.buildDeleteRequest(
                    new GenericUrl(importCmd));
        }
        restRequest.setReadTimeout(0);

        //logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
        	//response = restRequest.execute();
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public HttpResponse getEnvConfig(ServerProfile profile, 
                                                String resource) 
            throws IOException {

        HttpRequest restRequest = REQUEST_FACTORY
                .buildGetRequest(new GenericUrl(profile.getHostUrl() + "/"
                        + profile.getApi_version() + "/organizations/"
                        + profile.getOrg() + "/environments/"
                        + profile.getEnvironment() + "/" + resource));
        restRequest.setReadTimeout(0);
        
        //logger.debug(PrintUtil.formatRequest(restRequest));

        HttpResponse response = null;
        try {
        	//response = restRequest.execute();
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) return null;
            logger.error(e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public HttpResponse getEnvConfig(ServerProfile profile,
                                            String resource,
                                            String resourceId,
                                            String subResource,
                                            String subResourceId)
            throws IOException {

        String importCmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg() + "/environments/"
                + profile.getEnvironment()  + "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource + "/"
                + subResourceId;

        return executeAPIGet(profile, importCmd);
    }
    
	public HttpResponse patchEnvConfig(ServerProfile profile, 
            String resource,
            String resourceId,
            String payload)
	throws IOException {
	
		ByteArrayContent content = new ByteArrayContent("application/json", 
		                        payload.getBytes());
		
		String importCmd = profile.getHostUrl() + "/"
		+ profile.getApi_version() + "/organizations/"
		+ profile.getOrg() + "/environments/"
		+ profile.getEnvironment() + "/" + resource + "/"
		+ URLEncoder.encode(resourceId, "UTF-8");
		
		HttpRequest restRequest = APACHE_REQUEST_FACTORY.buildRequest(HttpMethods.PATCH, new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);
		
		//logger.info(PrintUtil.formatRequest(restRequest));
		
		HttpResponse response;
		try {
			//response = restRequest.execute();
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
    public HttpResponse createOrgConfig(ServerProfile profile, 
                                                String resource,
                                                String payload)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json", 
                                                            payload.getBytes());

        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/" + resource;

        HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(
                new GenericUrl(importCmd), content);
        restRequest.setReadTimeout(0);
        
        //logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
        	//response = restRequest.execute();
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public HttpResponse createOrgConfig(ServerProfile profile,
                                               String resource,
                                               String resourceId,
                                               String subResource,
                                               String payload)
            throws IOException {

        String importCmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg()+ "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource;

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

		//logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			//response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

    public HttpResponse updateOrgConfig(ServerProfile profile, 
                                                String resource,
                                                String resourceId,
                                                String payload)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json", 
                                                            payload.getBytes());

        String importCmd = profile.getHostUrl() + "/"
                                + profile.getApi_version() + "/organizations/"
                                + profile.getOrg() + "/" + resource + "/"
                                + URLEncoder.encode(resourceId, "UTF-8");

        HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(
                new GenericUrl(importCmd), content);
        restRequest.setReadTimeout(0);
        
        //logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
        	//response = restRequest.execute();
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public HttpResponse updateOrgConfig(ServerProfile profile,
                                               String resource,
                                               String resourceId,
                                               String subResource,
                                               String subResourceId,
                                               String payload)
            throws IOException {

        String cmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg() + "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource + "/"
                + subResourceId;

        return executeAPIPut(profile, payload, cmd);
    }
    
	public HttpResponse updateOrgConfigUpload(ServerProfile profile, 
													String resource,
													String resourceId,
													String filePath) throws IOException {

		byte[] file = Files.readAllBytes(new File(filePath).toPath());
		ByteArrayContent content = new ByteArrayContent("application/octet-stream", file);
		
		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
		+ "/" + resource+"/"+resourceId;

		HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		//logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			//response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

    public HttpResponse deleteOrgConfig(ServerProfile profile, 
                                                String resource,
                                                String resourceId,
                                                String subResource,
                                                String subResourceId)
            throws IOException {
        
        String cmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg() + "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource + "/"
                + subResourceId;

        HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(
                                                    new GenericUrl(cmd));
        restRequest.setReadTimeout(0);

        HttpResponse response;
        try {
        	//response = restRequest.execute();
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }
    
    public HttpResponse deleteOrgConfig(ServerProfile profile, 
                                                String resource,
                                                String resourceId)
            throws IOException {

        String importCmd = profile.getHostUrl() + "/"
                                + profile.getApi_version() + "/organizations/"
                                + profile.getOrg() + "/" + resource + "/"
                                + URLEncoder.encode(resourceId, "UTF-8");

        HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(
                                                    new GenericUrl(importCmd));
        restRequest.setReadTimeout(0);

        //logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
        	//response = restRequest.execute();
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

		//logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			//response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

    public HttpResponse getOrgConfig(ServerProfile profile, 
                                                String resource) 
            throws IOException {

        HttpRequest restRequest = REQUEST_FACTORY.buildGetRequest(
                new GenericUrl(profile.getHostUrl() + "/"
                        + profile.getApi_version() + "/organizations/"
                        + profile.getOrg() + "/" + resource));
        restRequest.setReadTimeout(0);

        //logger.debug(PrintUtil.formatRequest(restRequest));

        HttpResponse response = null;
        try {
        	//response = restRequest.execute();
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) return null;
            logger.error(e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public HttpResponse getOrgConfig(ServerProfile profile,
                                            String resource,
                                            String resourceId,
                                            String subResource,
                                            String subResourceId)
            throws IOException {

        String importCmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg()  + "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource + "/"
                + subResourceId;

        return executeAPIGet(profile, importCmd);
    }
    
    public HttpResponse getOrgConfig(ServerProfile profile,
                                            String resource,
                                            String resourceId,
                                            String subResource)
            throws IOException {

        String importCmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg()  + "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource;

        return executeAPIGet(profile, importCmd);
    }
    
    
    public HttpResponse patchOrgConfig(ServerProfile profile, 
            String resource,
            String payload)
	throws IOException {
	
		ByteArrayContent content = new ByteArrayContent("application/json", 
		                        payload.getBytes());
		
		String importCmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg() + "/" + resource;
		
		HttpRequest restRequest = APACHE_REQUEST_FACTORY.buildRequest(HttpMethods.PATCH, new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);
		
		//logger.info(PrintUtil.formatRequest(restRequest));
		
		HttpResponse response;
		try {
			//response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

	return response;
	}
    
    public HttpResponse patchOrgConfig(ServerProfile profile, 
                                                String resource,
                                                String resourceId,
                                                String payload)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json", 
                                                            payload.getBytes());

        String importCmd = profile.getHostUrl() + "/"
                                + profile.getApi_version() + "/organizations/"
                                + profile.getOrg() + "/" + resource + "/"
                                + URLEncoder.encode(resourceId, "UTF-8");

        HttpRequest restRequest = APACHE_REQUEST_FACTORY.buildRequest(HttpMethods.PATCH, new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);
        
        //logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
        	//response = restRequest.execute();
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    /***************************************************************************
     * API Config - get, create, update
     **/
        public HttpResponse createAPIConfig(ServerProfile profile, 
                                                    String api,
                                                    String resource,
                                                    String payload)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json", 
                                                            payload.getBytes());

        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/apis/"
                            + api + "/" + resource;

        HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(
                new GenericUrl(importCmd), content);
        restRequest.setReadTimeout(0);

        //logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
        	//response = restRequest.execute();
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public HttpResponse createAPIConfig(ServerProfile profile,
                                               String api,
                                               String resource,
                                               String resourceId,
                                               String subResource,
                                               String payload)
            throws IOException {

        String importCmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg() + "/apis/"
                + api+ "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource;

        return executeAPIPost(profile, payload, importCmd);
    }
        
        public HttpResponse createAPIConfigUpload(ServerProfile profile, String api, String resource, String filePath)
    			throws IOException {
    		byte[] file = Files.readAllBytes(new File(filePath).toPath());
    		ByteArrayContent content = new ByteArrayContent("application/octet-stream", file);

    		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
    							+ "/apis/" + api
    							+ "/" + resource;

    		HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(new GenericUrl(importCmd), content);
    		restRequest.setReadTimeout(0);

    		//logger.info(PrintUtil.formatRequest(restRequest));

    		HttpResponse response;
    		try {
    			//response = restRequest.execute();
    			response = executeAPI(profile, restRequest);
    		} catch (HttpResponseException e) {
    			logger.error("Apigee call failed " + e.getMessage());
    			throw new IOException(e.getMessage());
    		}

    		return response;
    	}

    public HttpResponse updateAPIConfig(ServerProfile profile, 
                                                String api,
                                                String resource,
                                                String resourceId,
                                                String payload)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json", 
                                                            payload.getBytes());

        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/apis/"
                            + api + "/" + resource + "/"
                            + URLEncoder.encode(resourceId, "UTF-8");

        HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(
                new GenericUrl(importCmd), content);
        restRequest.setReadTimeout(0);

        //logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
        	//response = restRequest.execute();
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public HttpResponse updateAPIConfig(ServerProfile profile,
                                               String api,
                                               String resource,
                                               String resourceId,
                                               String subResource,
                                               String subResourceId,
                                               String payload)
            throws IOException {

        String cmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg() + "/apis/"
                + api + "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource + "/"
                + subResourceId;

        return executeAPIPut(profile, payload, cmd);
    }
    
    public HttpResponse updateAPIConfigUpload(ServerProfile profile, String api, String resource, String resourceId,
			String filePath) throws IOException {

		byte[] file = Files.readAllBytes(new File(filePath).toPath());
		ByteArrayContent content = new ByteArrayContent("application/octet-stream", file);

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
							+ "/apis/"+ api
							+ "/" + resource + "/" + resourceId;

		HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(new GenericUrl(importCmd), content);
		restRequest.setReadTimeout(0);

		//logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			//response = restRequest.execute();
			response = executeAPI(profile, restRequest);
		} catch (HttpResponseException e) {
			logger.error("Apigee call failed " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return response;
	}

    public HttpResponse deleteAPIConfig(ServerProfile profile, 
                                                String api,
                                                String resource,
                                                String resourceId)
            throws IOException {


        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/apis/"
                            + api + "/" + resource + "/"
                            + URLEncoder.encode(resourceId, "UTF-8");

        HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(
                new GenericUrl(importCmd));
        restRequest.setReadTimeout(0);

        //logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
        	//response = restRequest.execute();
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public HttpResponse getAPIConfig(ServerProfile profile,
                                                String api,
                                                String resource) 
            throws IOException {

        HttpRequest restRequest = REQUEST_FACTORY
                .buildGetRequest(new GenericUrl(profile.getHostUrl() + "/"
                        + profile.getApi_version() + "/organizations/"
                        + profile.getOrg() + "/apis/"
                        + api + "/" + resource));
        restRequest.setReadTimeout(0);
        
        //logger.debug(PrintUtil.formatRequest(restRequest));

        HttpResponse response = null;
        try {
        	//response = restRequest.execute();
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) return null;
            logger.error(e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public HttpResponse getAPIConfig(ServerProfile profile,
                                            String api,
                                            String resource,
                                            String resourceId,
                                            String subResource,
                                            String subResourceId)
            throws IOException {

        String importCmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg() + "/apis/"
                + api + "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource + "/"
                + subResourceId;

        return executeAPIGet(profile, importCmd);
    }

    public HttpResponse deleteAPIResourceFileConfig(ServerProfile profile, String api, String resource, String resourceId)
			throws IOException {

		String importCmd = profile.getHostUrl() + "/" + profile.getApi_version() + "/organizations/" + profile.getOrg()
				+ "/apis/"+api
				+ "/" + resource + "/" + resourceId;

		HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(new GenericUrl(importCmd));
		restRequest.setReadTimeout(0);

		//logger.info(PrintUtil.formatRequest(restRequest));

		HttpResponse response;
		try {
			//response = restRequest.execute();
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
	
	        HttpRequest restRequest = REQUEST_FACTORY
	                .buildGetRequest(new GenericUrl(profile.getHostUrl() + "/"
	                        + profile.getApi_version() + "/organizations/"
	                        + profile.getOrg() + "/apis/"));
	        restRequest.setReadTimeout(0);
	
	        try {
	            HttpResponse response = executeAPI(profile, restRequest);            
	            //ignore response - we just wanted the MFA initialised
	            logger.info("=============MFA Initialised================");
	        } catch (HttpResponseException e) {
	            logger.error(e.getMessage());
	            //throw error as there is no point in continuing
	            throw e;
	        }
    	}
    }

    private HttpResponse executeAPIGet(ServerProfile profile, String importCmd)
            throws IOException {

        HttpRequest restRequest = REQUEST_FACTORY
                .buildGetRequest(
                        new GenericUrl(importCmd));
        restRequest.setReadTimeout(0);

        HttpResponse response;
        try {
            response = executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) return null;
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    private HttpResponse executeAPIPost(ServerProfile profile, String payload,
                                               String importCmd)
            throws IOException {

       
        return executeAPIPost(profile, payload, importCmd, "application/json");
    }

    private HttpResponse executeAPIPost(ServerProfile profile, String payload,
                                               String importCmd, String contentType)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent(contentType,
                payload.getBytes());

        HttpRequest restRequest = REQUEST_FACTORY
                .buildPostRequest(
                        new GenericUrl(importCmd), content);
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
    
    private HttpResponse executeAPIPut(ServerProfile profile, String payload,
			            						String cmd)
			throws IOException {

    	ByteArrayContent content = new ByteArrayContent("application/json",
    			payload.getBytes());
	
    	HttpRequest restRequest = REQUEST_FACTORY
    				.buildPutRequest(new GenericUrl(cmd), content);
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
     * 
     * @param profile
     * @param request
     * @return
     * @throws IOException
     */
    private HttpResponse executeAPI(ServerProfile profile, HttpRequest request) 
            throws IOException {
    	HttpHeaders headers = request.getHeaders();
    	try {
    		if(profile.getBearerToken()!=null && !profile.getBearerToken().equalsIgnoreCase("")) {
    			logger.info("Using the bearer token");
    			accessToken = profile.getBearerToken();
    		}
    		else if(profile.getServiceAccountJSONFile()!=null && !profile.getServiceAccountJSONFile().equalsIgnoreCase("")) {
    			logger.info("Using the service account file to generate a token");
    			File serviceAccountJSON = new File(profile.getServiceAccountJSONFile());
    			accessToken = getGoogleAccessToken(serviceAccountJSON);
    		}
    		else {
    			logger.error("Service Account file or bearer token is missing");
				throw new IOException("Service Account file or bearer token is missing");
    		}
            logger.debug("**Access Token** "+ accessToken);
    		headers.setAuthorization("Bearer " + accessToken);
    	}catch (Exception e) {
            logger.error(e.getMessage());
            throw new IOException(e.getMessage());
         }
    	//fix for Issue106
    	headers.set("X-GOOG-API-FORMAT-VERSION", 2);
    	logger.info(PrintUtil.formatRequest(request));
        return request.execute();
    }
    
    /**
	 * To get the Google Service Account Access Token
	 * 
	 * @param serviceAccountFilePath
	 * @return
	 * @throws Exception
	 */
	private String getGoogleAccessToken(File serviceAccountJSON) throws IOException {
		String tokenUrl = "https://oauth2.googleapis.com/token";
		long now = System.currentTimeMillis();
		try {
			ServiceAccountCredentials serviceAccount = ServiceAccountCredentials.fromStream(new FileInputStream(serviceAccountJSON));
			Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey)serviceAccount.getPrivateKey());
			String signedJwt = JWT.create()
	                .withKeyId(serviceAccount.getPrivateKeyId())
	                .withIssuer(serviceAccount.getClientEmail())
	                .withAudience(tokenUrl)
	                .withClaim("scope","https://www.googleapis.com/auth/cloud-platform")
	                .withIssuedAt(new Date(now))
	                .withExpiresAt(new Date(now + 3600 * 1000L))
	                .sign(algorithm);
			//System.out.println(signedJwt);
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
			params.put("assertion", signedJwt);
			HttpContent content = new UrlEncodedContent(params);
			
			HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(new GenericUrl(tokenUrl), content);
	        restRequest.setReadTimeout(0);
	        HttpResponse response = restRequest.execute();
	        String payload = response.parseAsString();
            JSONParser parser = new JSONParser();       
            JSONObject obj     = (JSONObject)parser.parse(payload);
            return (String)obj.get("access_token");
		}catch (Exception e) {
			logger.error(e.getMessage());
            throw new IOException(e.getMessage());
		}
	}
    
}
