/**
 * Copyright 2024 Google LLC
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
package com.apigee.edge.config.mavenplugin;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.apigee.edge.config.rest.RestUtil;
import com.apigee.edge.config.utils.ServerProfile;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.util.Key;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**                                                                                                                                     ¡¡
 * To important existing consumer keys and secrets
 * scope: org
 *
 * @author ssvaidyanathan
 * @goal importAppGroupAppKeys
 * @phase install
 */

public class ImportAppGroupAppKeysMojo extends GatewayAbstractMojo
{
	static Logger logger = LogManager.getLogger(ImportAppGroupAppKeysMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	private ServerProfile serverProfile;

    public static class AppGroupApp {
        @Key
        public String name;
        @Key
        public String consumerKey;
        @Key
        public String consumerSecret;
        @Key
        public List<String> apiProducts;
    }
    
    public static class AppCreds {
        @Key
        public String consumerKey;
        @Key
        public String consumerSecret;
    }
    
    public static class ApiProducts {
    	@Key
        public List<String> apiProducts;
    }
	
	public ImportAppGroupAppKeysMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Import Consumer Keys and Secrets for App group Apps");
			logger.info(____ATTENTION_MARKER____);

			serverProfile = super.getProfile();	
			logger.debug("Base dir " + super.getBaseDirectoryPath());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Invalid apigee.option provided");
		} catch (RuntimeException e) {
			throw e;
		}

	}

	protected String getAppName(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			AppGroupApp app = gson.fromJson(payload, AppGroupApp.class);
			return app.name;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}
	
	public static String getApiProducts(String payload) 
            throws IOException {
		Gson gson = new Gson();
		try {
			AppGroupApp app = gson.fromJson(payload, AppGroupApp.class);
			ApiProducts products = new ApiProducts();
			products.apiProducts = app.apiProducts;
			return gson.toJson(products);
		} catch (JsonParseException e) {
		  throw new IOException(e.getMessage());
		}
	}
	
	public static String getAppKey(String payload) 
            throws IOException {
		Gson gson = new Gson();
		try {
			AppGroupApp app = gson.fromJson(payload, AppGroupApp.class);
			return app.consumerKey;
		} catch (JsonParseException e) {
		  throw new IOException(e.getMessage());
		}
	}
	
	
	public static String getAppCreds(String payload) 
            throws IOException {
		Gson gson = new Gson();
		try {
			AppGroupApp app = gson.fromJson(payload, AppGroupApp.class);
			AppCreds creds = new AppCreds();
			creds.consumerKey = app.consumerKey;
			creds.consumerSecret = app.consumerSecret;
			return gson.toJson(creds);
		} catch (JsonParseException e) {
		  throw new IOException(e.getMessage());
		}
	}

	protected void doImport(Map<String, List<String>> appGroupApps) 
            throws MojoFailureException {
		try {
            for (Map.Entry<String, List<String>> entry : appGroupApps.entrySet()) {
                String appGroupId = URLEncoder.encode(entry.getKey(), "UTF-8");
    	        for (String app : entry.getValue()) {
    	        	String appName = getAppName(app);
    	        	if (appName == null) {
    	        		throw new IllegalArgumentException(
    	        			"App Group App does not have a name.\n" + app + "\n");
    	        	}
    	        	if (doesAppGroupAppExist(serverProfile, appGroupId, appName)) {
            			logger.info("Create the consumer key and secret");
            			createConsumerKeyAndSecret(serverProfile, appGroupId, appName, app);
            			
            			logger.info("Associate the consumer key/secret with API Products");
            			associateAPIProductToKey(serverProfile, appGroupId, appName, app);
            		}
            		else {
            			logger.error("App Group App "+ appName +" does not exist");
            			throw new IllegalArgumentException("App Group App "+ appName +" does not exist");
            		}
    			}
            }
		
		} catch (IOException e) {
			throw new MojoFailureException("Apigee network call error " +
														 e.getMessage());
		} catch (RuntimeException e) {
			throw e;
		}
	}

	/** 
	 * Entry point for the mojo.
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (super.isSkip()) {
			logger.info("Skipping");
			return;
		}

		Logger logger = LogManager.getLogger(ImportAppGroupAppKeysMojo.class);

		try {
			
			init();

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			Map<String, List<String>> apps = getOrgConfigWithId(logger, "importAppGroupAppKeys");
			if (apps == null || apps.size() == 0) {
				logger.info("No import Keys found.");
                return;
			}

            logger.debug(apps.toString());
			doImport(apps);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
	
	 public static String createConsumerKeyAndSecret(ServerProfile profile, String appGroupId, String appName, String app)
            		 throws IOException {
		 String creds = getAppCreds(app);
		 RestUtil restUtil = new RestUtil(profile);
		 HttpResponse response = restUtil.createOrgConfig(profile, 
                 "appgroups/" + appGroupId + "/apps/"+appName+"/keys",
                 creds);
		try {
		
			logger.info("Response " + response.getContentType() + "\n" +
			                 response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Import Consumer Key/Secret Success.");
		
		} catch (HttpResponseException e) {
			logger.error("Import Consumer Key/Secret error " + e.getMessage());
			throw new IOException(e.getMessage());
		}
		
		return "";
	 }
	 
	 
	 public static String associateAPIProductToKey(ServerProfile profile, String appGroupId, String appName, String app)
	    		 throws IOException {
		 String apiProducts = getApiProducts(app);
		 String appKey = getAppKey(app);
		 RestUtil restUtil = new RestUtil(profile);
		 HttpResponse response = restUtil.createOrgConfig(profile, 
                 "appgroups/" + appGroupId + "/apps/"+appName+"/keys/"+appKey,
                 apiProducts);
		try {
		
			logger.info("Response " + response.getContentType() + "\n" +
			                 response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Association of API Products to Consumer Key Success.");
		
		} catch (HttpResponseException e) {
			logger.error("Association of API Products to Consumer Key error " + e.getMessage());
			throw new IOException(e.getMessage());
		}
		
		return "";
	 }

	 public static List getApp(ServerProfile profile, String appGroupId)
	            throws IOException {
		RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, 
                                        "appgroups/" + appGroupId + "/apps");
        if(response == null) return new ArrayList();

        JSONArray apps = new JSONArray();
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);
            JSONParser parser = new JSONParser();       
            JSONObject obj     = (JSONObject)parser.parse(payload);
            JSONArray appsArray    = (JSONArray)obj.get("appGroups");
            for (int i = 0; appsArray != null && i < appsArray.size(); i++) {
             	 JSONObject a = (JSONObject) appsArray.get(i);
             	 apps.add(a.get("name"));
           }
        } catch (ParseException pe){
            logger.error("Get App parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Apps error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return apps;
    }
	 
	 public static boolean doesAppGroupAppExist(ServerProfile profile, String appGroupId, String appName)
            throws IOException {
        try {
        	RestUtil restUtil = new RestUtil(profile);
        	logger.info("Checking if App Group App - " +appName + " exist");
            HttpResponse response = restUtil.getOrgConfig(profile, "appgroups/"+appGroupId+"/apps/"+appName);
            if(response == null) 
            	return false;
        } catch (HttpResponseException e) {
            throw new IOException(e.getMessage());
        }

        return true;
    }
	 
}



