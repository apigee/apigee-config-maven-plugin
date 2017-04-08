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
package com.apigee.edge.config.mavenplugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apigee.edge.config.rest.RestUtil;
import com.apigee.edge.config.utils.ServerProfile;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.util.Key;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**                                                                                                                                     ¡¡
 * Goal to create Apps in Apigee EDGE
 * scope: org
 *
 * @author madhan.sadasivam
 * @goal apps
 * @phase install
 */

public class AppMojo extends GatewayAbstractMojo
{
	static Logger logger = LoggerFactory.getLogger(AppMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class App {
        @Key
        public String name;
        @Key
        public String consumerKey;
        @Key
        public String consumerSecret;
        @Key
        public List<String> apiProducts;
    }
    
    public static class Credentials {
        @Key
        public String consumerKey;
    }
    
    public static class DevAppResponse {
        @Key
        public String name;
        @Key
        public List<Credentials> credentials;
    }
	
	public AppMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee App");
			logger.info(____ATTENTION_MARKER____);

			String options="";
			serverProfile = super.getProfile();			
	
			options = super.getOptions();
			if (options != null) {
				buildOption = OPTIONS.valueOf(options);
			}
			logger.debug("Build option " + buildOption.name());
			logger.debug("Base dir " + super.getBaseDirectoryPath());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Invalid apigee.option provided");
		} catch (RuntimeException e) {
			throw e;
		}

	}

	protected App getAppObj(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			App app = gson.fromJson(payload, App.class);
			return app;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}
	
	private static DevAppResponse getDevAppResponseObj(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			DevAppResponse devAppResponseObj = gson.fromJson(payload, DevAppResponse.class);
			return devAppResponseObj;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	private static void deleteAppKeys(ServerProfile profile, String developerId, App appObj, String responseStr)
			throws IOException, MojoFailureException {

		DevAppResponse devAppResponseObj = null;
		devAppResponseObj = getDevAppResponseObj(responseStr);
		if (devAppResponseObj != null && devAppResponseObj.credentials != null
				&& devAppResponseObj.credentials.size() > 0) {
			for (Credentials cred : devAppResponseObj.credentials) {
				logger.info("Deleting " + cred.consumerKey);
				deleteAPIProductToKey(profile, developerId, appObj, cred.consumerKey);
			}
		}
	}
	
	protected void doUpdate(Map<String, List<String>> devApps) 
            throws MojoFailureException {
		try {
			List existingApps = null;
			if (buildOption != OPTIONS.update && 
                buildOption != OPTIONS.create &&
				buildOption != OPTIONS.delete &&
                buildOption != OPTIONS.sync) {
				return;
			}

            for (Map.Entry<String, List<String>> entry : devApps.entrySet()) {

                String developerId = entry.getKey();
                logger.info("Retrieving Apps of " + developerId);
                existingApps = getApp(serverProfile, developerId);

    	        for (String app : entry.getValue()) {
    	        	App appObj = getAppObj(app);
    	        	String appName = appObj.name;
    	        	if (appName == null) {
    	        		throw new IllegalArgumentException(
    	        			"App does not have a name.\n" + app + "\n");
    	        	}

            		if (existingApps.contains(appName)) {
                        switch (buildOption) {
                            case update:
                                logger.info("App \"" + appName + 
                                                        "\" exists. Updating.");
                                
                                updateApp(serverProfile, developerId,
                                                        appName, app, appObj);
                                break;
                            case create:
                                logger.info("App \"" + appName + 
                                                "\" already exists. Skipping.");
                                break;
                            case delete:
                                logger.info("App \"" + appName + 
                                                "\" already exists. Deleting.");
                                deleteApp(serverProfile, developerId, appName);
                                break;
                            case sync:
                                logger.info("App \"" + appName + 
                                                "\" already exists. Deleting and recreating.");
                                deleteApp(serverProfile, developerId, appName);
                                logger.info("Creating App - " + appName);
                                createApp(serverProfile, developerId, app, appObj);
                                break;
                        }
    	        	} else {
                        switch (buildOption) {
                            case create:
                            case sync:
                            case update:
                                logger.info("Creating App - " + appName);
                                createApp(serverProfile, developerId, app, appObj);
                                break;
                            case delete:
                                logger.info("App \"" + appName + 
                                                "\" does not exist. Skipping.");
                                break;
                        }
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
			getLog().info("Skipping");
			return;
		}

		Logger logger = LoggerFactory.getLogger(AppMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Apps (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			Map<String, List<String>> apps = getOrgConfigWithId(logger, "developerApps");
			if (apps == null || apps.size() == 0) {
				logger.info("No developers apps found.");
                return;
			}

            logger.debug(apps.toString());
			doUpdate(apps);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createApp(ServerProfile profile, 
                                    String developerId,
                                    String app,
                                    App appObj)
            throws IOException, MojoFailureException {

        HttpResponse response = RestUtil.createOrgConfig(profile, 
                                        "developers/" + developerId + "/apps",
                                         app);
        String responseStr = null;
        try {
        	responseStr = response.parseAsString();
        	logger.info("Response " + response.getContentType() + "\n" + responseStr);
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("App create error " + e.getMessage());
            throw new IOException(e.getMessage());
        } 
        
        //If consumerKey and consumerSecret is being passed in the configurations
        	//Create the consumerKey and consumerSecret
        	//Add API Products to Key
        	//Delete the Key that was generated while creating the app so that only one key is available for the dev app
        if(appObj!=null && appObj.consumerKey!=null && appObj.consumerSecret!=null){
        	logger.info("Create Consumer Key and Secret");
        	createConsumerKeySecret(profile, developerId, appObj);
        	logger.info("Add API Product to Key");
        	addAPIProductToKey(profile, developerId, appObj);
        	logger.info("Delete API Product to Generated Key");
        	logger.info("Delete Existing API Keys");
	    	deleteAppKeys(profile, developerId, appObj, responseStr);
        }
        
        return "";
    }
    
    public static String updateApp(ServerProfile profile,
                                    String developerId, 
                                    String appName, 
                                    String app,
                                    App appObj)
            throws IOException, MojoFailureException {

    	HttpResponse response = null;
        String responseStr = null;
        
        if(appObj!=null && appObj.consumerKey!=null && appObj.consumerSecret!=null){
	    	//Delete all the App Keys first before updating the app
	        logger.info("Getting Existing API Keys");
	    	response = RestUtil.getOrgConfig(profile, 
	                "developers/" + developerId + "/apps/"+appName);
	    	
	    	try{
	    		responseStr = response.parseAsString();
	    		logger.info("Delete Existing API Keys");
	    		deleteAppKeys(profile, developerId, appObj, responseStr);
	    	} catch (HttpResponseException e) {
	            logger.error("App update error " + e.getMessage());
	            throw new IOException(e.getMessage());
	        }
        }
    	logger.info("Updating App");
    	response = RestUtil.updateOrgConfig(profile, 
                                        "developers/" + developerId + "/apps", 
                                        appName,
                                        app);
        try {
        	responseStr = response.parseAsString();
            logger.info("Response " + response.getContentType() + "\n" + responseStr);
            if (response.isSuccessStatusCode())
            	logger.info("Update Success.");
            //If consumerKey and consumerSecret is being passed in the configurations
        	//Create the consumerKey and consumerSecret
        	//Add API Products to Key
            //Delete the generated key
    	    if(appObj!=null && appObj.consumerKey!=null && appObj.consumerSecret!=null){
    	    	logger.info("Create Consumer Key and Secret");
    	    	createConsumerKeySecret(profile, developerId, appObj);
    	    	logger.info("Add API Product to Key");
    	    	addAPIProductToKey(profile, developerId, appObj);
    	    	logger.info("Delete Existing API Keys");
    	    	deleteAppKeys(profile, developerId, appObj, responseStr);
    	    }
        } catch (HttpResponseException e) {
            logger.error("App update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteApp(ServerProfile profile,
                                    String developerId, 
                                    String appName)
            throws IOException {

        HttpResponse response = RestUtil.deleteOrgConfig(profile, 
                                        "developers/" + developerId + "/apps", 
                                        appName);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("App delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }
    
    public static String createConsumerKeySecret(ServerProfile profile, String developerId, App appObj) throws IOException {

		String payload = "{"+
				  "\"consumerKey\": \""+appObj.consumerKey+"\","+
				  "\"consumerSecret\": \""+appObj.consumerSecret+"\""+
				  "}";
		HttpResponse response = RestUtil.createOrgConfig(profile, "developers/" + developerId + "/apps/"+ appObj.name+"/keys/create", payload);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Create Success.");

		} catch (HttpResponseException e) {
			logger.error("Consumer Key create error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}
	
	public static String addAPIProductToKey(ServerProfile profile, String developerId, App appObj) throws IOException {

		String payload = "{"+
				  	"\"apiProducts\":"+new Gson().toJson(appObj.apiProducts)+
				  "}";
		HttpResponse response = RestUtil.createOrgConfig(profile, "developers/" + developerId + "/apps/"+ appObj.name+"/keys/"+appObj.consumerKey, payload);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Create Success.");

		} catch (HttpResponseException e) {
			logger.error("Add API Product to Key error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}
    
    public static String deleteAPIProductToKey(ServerProfile profile, String developerId, App appObj, String consumerKey) throws IOException {
		HttpResponse response = RestUtil.deleteOrgConfig(profile, "developers/" + developerId + "/apps/"+ appObj.name+"/keys", consumerKey);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Delete Success.");

		} catch (HttpResponseException e) {
			logger.error("Delete API Product to Key error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}

    public static List getApp(ServerProfile profile, String developerId)
            throws IOException {

        HttpResponse response = RestUtil.getOrgConfig(profile, 
                                        "developers/" + developerId + "/apps");
        if(response == null) return new ArrayList();

        JSONArray apps = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"apps\": " + payload + "}";

            JSONParser parser = new JSONParser();                
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            apps    = (JSONArray)obj1.get("apps");

        } catch (ParseException pe){
            logger.error("Get App parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Apps error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return apps;
    }	
}




