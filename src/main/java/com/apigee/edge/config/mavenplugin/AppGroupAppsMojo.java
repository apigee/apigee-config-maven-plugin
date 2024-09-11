/**
 * Copyright (C) 2024 Google LLC
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**                                                                                                                                     ¡¡
 * Goal to create App Group Apps in Apigee
 * scope: org
 *
 * @author ssvaidyanathan
 * @goal appgroupapps
 * @phase install
 */

public class AppGroupAppsMojo extends GatewayAbstractMojo
{
	static Logger logger = LogManager.getLogger(AppGroupAppsMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class AppGroupApp {
        @Key
        public String name;
    }
	
	public AppGroupAppsMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee App Group App");
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

	protected String getAppGroupAppName(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			AppGroupApp app = gson.fromJson(payload, AppGroupApp.class);
			return app.name;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(Map<String, List<String>> appGroupApps) 
            throws MojoFailureException {
		try {
			if (buildOption != OPTIONS.update && 
                buildOption != OPTIONS.create &&
				buildOption != OPTIONS.delete &&
                buildOption != OPTIONS.sync) {
				return;
			}

            for (Map.Entry<String, List<String>> entry : appGroupApps.entrySet()) {
                String appGroupId = URLEncoder.encode(entry.getKey(), "UTF-8");
                
    	        for (String appGroupApp : entry.getValue()) {
    	        	String appGroupAppName = getAppGroupAppName(appGroupApp);
    	        	if (appGroupAppName == null) {
    	        		throw new IllegalArgumentException(
    	        			"App Group App does not have a name.\n" + appGroupApp + "\n");
    	        	}

    	        	if (doesAppGroupAppExist(serverProfile, appGroupId, appGroupAppName)) {
                        switch (buildOption) {
                            case update:
                                logger.info("App Group App \"" + appGroupAppName + 
                                                        "\" exists. Updating.");
                                updateAppGroupApp(serverProfile, appGroupId,
                                		appGroupAppName, appGroupApp);
                                break;
                            case create:
                                logger.info("App Group App \"" + appGroupAppName + 
                                                "\" already exists. Skipping.");
                                break;
                            case delete:
                                logger.info("App Group App \"" + appGroupAppName + 
                                                "\" already exists. Deleting.");
                                deleteAppGroupApp(serverProfile, appGroupId, appGroupAppName);
                                break;
                            case sync:
                                logger.info("App Group App \"" + appGroupAppName + 
                                                "\" already exists. Deleting and recreating.");
                                deleteAppGroupApp(serverProfile, appGroupId, appGroupAppName);
                                logger.info("Creating App Group App - " + appGroupAppName);
                                createAppGroupApp(serverProfile, appGroupId, appGroupApp);
                                break;
                        }
    	        	} else {
                        switch (buildOption) {
                            case create:
                            case sync:
                            case update:
                                logger.info("Creating App Group App - " + appGroupAppName);
                                createAppGroupApp(serverProfile, appGroupId, appGroupApp);
                                break;
                            case delete:
                                logger.info("App Group App \"" + appGroupAppName + 
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
			logger.info("Skipping");
			return;
		}

		Logger logger = LogManager.getLogger(AppGroupAppsMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping App Group Apps (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			Map<String, List<String>> appGroupApps = getOrgConfigWithId(logger, "appGroupApps");
			if (appGroupApps == null || appGroupApps.size() == 0) {
				logger.info("No App Group apps found.");
                return;
			}

            logger.debug(appGroupApps.toString());
			doUpdate(appGroupApps);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createAppGroupApp(ServerProfile profile, 
                                    String appGroupId,
                                    String app)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createOrgConfig(profile, 
                                        "appgroups/" + appGroupId + "/apps",
                                         app);
        try {

            logger.debug("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("App Group App create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateAppGroupApp(ServerProfile profile,
                                    String appGroupId, 
                                    String appGroupAppName, 
                                    String appGroupApp)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.updateOrgConfig(profile, 
                                        "appgroups/" + appGroupId + "/apps", 
                                        appGroupAppName,
                                        removeApiProductFromApp(profile, appGroupApp));
        try {
            
            logger.debug("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("App Group App update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteAppGroupApp(ServerProfile profile,
                                    String appGroupId, 
                                    String appGroupAppName)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteOrgConfig(profile, 
                                        "appgroups/" + appGroupId + "/apps", 
                                        appGroupAppName);
        try {
            
            logger.debug("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("App Group App delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getAppGroupApp(ServerProfile profile, String appGroupId)
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
            JSONArray appsArray    = (JSONArray)obj.get("appGroupApps");
            for (int i = 0; appsArray != null && i < appsArray.size(); i++) {
             	 JSONObject a = (JSONObject) appsArray.get(i);
             	 apps.add(a.get("appId"));
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
    
    //Method to remove the apiProducts from App payload for config update option so that it does not create credentials everytime
    //https://github.com/apigee/apigee-config-maven-plugin/issues/128
    public static String removeApiProductFromApp(ServerProfile profile, String appPayload){
		if(profile.getIgnoreProductsForApp()) {
			logger.info("Ignoring the API Product config from App Group App");
			JsonParser parser = new JsonParser();
			JsonElement jsonElement = parser.parse(appPayload);
			JsonObject appJsonObj = jsonElement.getAsJsonObject();
			appJsonObj.remove("apiProducts");
			return appJsonObj.toString();
		}else
			return appPayload; 	
	}
    
    public static boolean doesAppGroupAppExist(ServerProfile profile, String appGroupId, String appGroupAppName)
            throws IOException {
        try {
        	RestUtil restUtil = new RestUtil(profile);
        	logger.info("Checking if appGroupApp - " +appGroupAppName + " exist");
            HttpResponse response = restUtil.getOrgConfig(profile, "appgroups/"+appGroupId+"/apps/"+appGroupAppName);
            if(response == null) 
            	return false;
        } catch (HttpResponseException e) {
            throw new IOException(e.getMessage());
        }

        return true;
    }	
}




