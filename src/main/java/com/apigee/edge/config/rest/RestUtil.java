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
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apigee.edge.config.utils.PrintUtil;
import com.apigee.edge.config.utils.ServerProfile;
import com.apigee.mgmtapi.sdk.client.MgmtAPIClient;
import com.apigee.mgmtapi.sdk.model.AccessToken;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

public class RestUtil {

    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();
    static String versionRevision;
    static Logger logger = LoggerFactory.getLogger(RestUtil.class);
    static String accessToken = null;
    
    static HttpRequestFactory REQUEST_FACTORY = HTTP_TRANSPORT
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

    /***************************************************************************
     * Env Config - get, create, update
     **/
    public static HttpResponse createEnvConfig(ServerProfile profile, 
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

    public static HttpResponse createEnvConfig(ServerProfile profile,
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

    public static HttpResponse createEnvConfigUpload(ServerProfile profile, String resource, String filePath)
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
    
    public static HttpResponse updateEnvConfig(ServerProfile profile, 
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

    public static HttpResponse updateEnvConfig(ServerProfile profile,
                                               String resource,
                                               String resourceId,
                                               String subResource,
                                               String subResourceId,
                                               String payload)
            throws IOException {

        String importCmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg() + "/environments/"
                + profile.getEnvironment() + "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource +"/"
                + subResourceId;

        return executeAPIPost(profile, payload, importCmd);
    }

	public static HttpResponse updateEnvConfigUpload(ServerProfile profile, String resource, String resourceId,
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
	
	public static HttpResponse deleteEnvResourceFileConfig(ServerProfile profile, String resource, String resourceId)
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
    
    public static HttpResponse deleteEnvConfig(ServerProfile profile, 
									            String resource,
									            String resourceId)
	throws IOException {
    	return deleteEnvConfig(profile, resource, resourceId, null);
    }
    
    public static HttpResponse deleteEnvConfig(ServerProfile profile, 
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
        	restRequest = REQUEST_FACTORY.buildRequest(HttpMethod.DELETE, new GenericUrl(importCmd), content);
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

    public static HttpResponse getEnvConfig(ServerProfile profile, 
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

    public static HttpResponse getEnvConfig(ServerProfile profile,
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

    /***************************************************************************
     * Org Config - get, create, update
     **/
    public static HttpResponse createOrgConfig(ServerProfile profile, 
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

    public static HttpResponse createOrgConfig(ServerProfile profile,
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
    
	public static HttpResponse createOrgConfigUpload(ServerProfile profile, String resource, String filePath)
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

    public static HttpResponse updateOrgConfig(ServerProfile profile, 
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

    public static HttpResponse updateOrgConfig(ServerProfile profile,
                                               String resource,
                                               String resourceId,
                                               String subResource,
                                               String subResourceId,
                                               String payload)
            throws IOException {

        String importCmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg() + "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource + "/"
                + subResourceId;

        return executeAPIPost(profile, payload, importCmd);
    }
    
	public static HttpResponse updateOrgConfigUpload(ServerProfile profile, 
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

    public static HttpResponse deleteOrgConfig(ServerProfile profile, 
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
    
	public static HttpResponse deleteOrgResourceFileConfig(ServerProfile profile, String resource, String resourceId)
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

    public static HttpResponse getOrgConfig(ServerProfile profile, 
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

    public static HttpResponse getOrgConfig(ServerProfile profile,
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

    /***************************************************************************
     * API Config - get, create, update
     **/
        public static HttpResponse createAPIConfig(ServerProfile profile, 
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

    public static HttpResponse createAPIConfig(ServerProfile profile,
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
        
        public static HttpResponse createAPIConfigUpload(ServerProfile profile, String api, String resource, String filePath)
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

    public static HttpResponse updateAPIConfig(ServerProfile profile, 
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

    public static HttpResponse updateAPIConfig(ServerProfile profile,
                                               String api,
                                               String resource,
                                               String resourceId,
                                               String subResource,
                                               String subResourceId,
                                               String payload)
            throws IOException {

        String importCmd = profile.getHostUrl() + "/"
                + profile.getApi_version() + "/organizations/"
                + profile.getOrg() + "/apis/"
                + api + "/" + resource + "/"
                + URLEncoder.encode(resourceId, "UTF-8")
                + "/" + subResource + "/"
                + subResourceId;

        return executeAPIPost(profile, payload, importCmd);
    }
    
    public static HttpResponse updateAPIConfigUpload(ServerProfile profile, String api, String resource, String resourceId,
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

    public static HttpResponse deleteAPIConfig(ServerProfile profile, 
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

    public static HttpResponse getAPIConfig(ServerProfile profile,
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

    public static HttpResponse getAPIConfig(ServerProfile profile,
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

    public static HttpResponse deleteAPIResourceFileConfig(ServerProfile profile, String api, String resource, String resourceId)
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
    
    public static void initMfa(ServerProfile profile) throws IOException {

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

    private static HttpResponse executeAPIGet(ServerProfile profile, String importCmd)
            throws IOException {

        HttpRequest restRequest = REQUEST_FACTORY
                .buildGetRequest(
                        new GenericUrl(importCmd));
        restRequest.setReadTimeout(0);

        HttpResponse response;
        try {
            response = RestUtil.executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) return null;
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    private static HttpResponse executeAPIPost(ServerProfile profile, String payload,
                                               String importCmd)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json",
                payload.getBytes());

        HttpRequest restRequest = REQUEST_FACTORY
                .buildPostRequest(
                        new GenericUrl(importCmd), content);
        restRequest.setReadTimeout(0);

        HttpResponse response;
        try {
            response = RestUtil.executeAPI(profile, restRequest);
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }
  
    /**
     * OAuth token acquisition for calling management APIs
     * Access Token expiry 1799 sec = 30 mins long enough to finish any maven task
     * MFA Token: TOTP expires in 30 secs. User needs to give a token with some validity
     */
    private static HttpResponse executeAPI(ServerProfile profile, HttpRequest request) 
            throws IOException {
        HttpHeaders headers = request.getHeaders();
        MgmtAPIClient client = new MgmtAPIClient();
        String mfaToken = profile.getMFAToken();
        String tokenUrl = profile.getTokenUrl();
        String mgmtAPIClientId = (profile.getClientId()!=null && !profile.getClientId().equalsIgnoreCase(""))?profile.getClientId():"edgecli";
        String mgmtAPIClientSecret = (profile.getClientSecret()!=null && !profile.getClientSecret().equalsIgnoreCase(""))?profile.getClientSecret():"edgeclisecret";
        headers.setAccept("application/json");
        /**** Basic Auth - Backward compatibility ****/
        if (profile.getAuthType() != null &&
            profile.getAuthType().equalsIgnoreCase("basic")) {
                headers.setBasicAuthentication(profile.getCredential_user(),
                                                profile.getCredential_pwd());
                logger.info(PrintUtil.formatRequest(request));
                return request.execute();
        }
        /**** OAuth ****/
        if (profile.getBearerToken() != null && !profile.getBearerToken().equalsIgnoreCase("")){
        	//Need to validate access token only if refresh token is provided. 
	        	//If access token is not valid, create a bearer token using the refresh token 
	        	//If access token is valid, use that 
        	accessToken = (accessToken!=null)?accessToken:profile.getBearerToken();        	
        	if(profile.getRefreshToken() != null && !profile.getRefreshToken().equalsIgnoreCase("")){
        		if(isValidBearerToken(accessToken, profile, mgmtAPIClientId)){
        			logger.info("Access Token valid");
        			headers.setAuthorization("Bearer " + accessToken);
                 }else{
                	 try{
                		 AccessToken token = null;
                		 logger.info("Access token not valid so acquiring new access token using Refresh Token");
                		 token = client.getAccessTokenFromRefreshToken(
		     			 			tokenUrl,
		     			 			mgmtAPIClientId, mgmtAPIClientSecret, 
		     			 			profile.getRefreshToken());
                		 logger.info("New Access Token acquired");
			         	 accessToken = token.getAccess_token();
			             headers.setAuthorization("Bearer " + accessToken);
                	 }catch (Exception e) {
                        logger.error(e.getMessage());
                        throw new IOException(e.getMessage());
                     }
                 }
        	}
        	//if refresh token is not passed, validate the access token and use it accordingly
        	else{
        		logger.info("Validating the access token passed");
        		if(isValidBearerToken(profile.getBearerToken(), profile, mgmtAPIClientId)){
        			logger.info("Access Token valid");
        			accessToken = profile.getBearerToken();
                    headers.setAuthorization("Bearer " + accessToken);
        		}else{
        			logger.error("Access token not valid");
        			throw new IOException ("Access token not valid");
        		}
        		
        	}
        }
        else if (accessToken != null) {
            // subsequent calls
            logger.debug("Reusing mgmt API access token");
            headers.setAuthorization("Bearer " + accessToken);
        } else {
            logger.info("Acquiring mgmt API token from " + tokenUrl);
            try {
                AccessToken token = null;
                if (mfaToken == null || mfaToken.length() == 0) {
                    logger.info("MFA token not provided. Skipping.");
                    token = client.getAccessToken(
                            tokenUrl,
                            mgmtAPIClientId, mgmtAPIClientSecret,
                            profile.getCredential_user(),
                            profile.getCredential_pwd());
                } else {
                    logger.info("Making use of the MFA token provided.");
                    token = client.getAccessToken(
                            tokenUrl,
                            mgmtAPIClientId, mgmtAPIClientSecret,
                            profile.getCredential_user(),
                            profile.getCredential_pwd(),
                            profile.getMFAToken());
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
     * This method is used to validate the Bearer token. It validates the source and the expiration and if the token is about to expire in 30 seconds, set as invalid token
     * @param accessToken
     * @param profile
     * @param clientId
     * @return
     * @throws IOException
     */
    private static boolean isValidBearerToken(String accessToken, ServerProfile profile, String clientId) throws IOException{
    	boolean isValid = false;
    	try {
		    JWT jwt = JWT.decode(accessToken);
		    String jwtClientId = jwt.getClaim("client_id").asString();
		    String jwtEmailId = jwt.getClaim("email").asString();
		    long jwtExpiresAt = jwt.getExpiresAt().getTime()/1000;
		    long difference = jwtExpiresAt - (System.currentTimeMillis()/1000);
		    if(jwt!= null && jwtClientId!=null && jwtClientId.equals(clientId)
	    		&& jwtEmailId!=null && jwtEmailId.equalsIgnoreCase(profile.getCredential_user())
	    		&& profile.getTokenUrl().contains(jwt.getIssuer())
	    		&& difference >= 30){
		    	isValid = true;
		    }
		} catch (JWTDecodeException exception){
		   throw new IOException(exception.getMessage());
		}
    	return isValid;
    }
    
}
