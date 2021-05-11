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
import java.net.URLEncoder;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

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

	protected String getAppName(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			App app = gson.fromJson(payload, App.class);
			return app.name;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(Map<String, List<String>> devApps) 
            throws MojoFailureException {
		try {
			//List existingApps = null;
			if (buildOption != OPTIONS.update && 
                buildOption != OPTIONS.create &&
				buildOption != OPTIONS.delete &&
                buildOption != OPTIONS.sync) {
				return;
			}

            for (Map.Entry<String, List<String>> entry : devApps.entrySet()) {

            	//logger.info("Retrieving Apps of " + entry.getKey());
                String developerId = URLEncoder.encode(entry.getKey(), "UTF-8");
                //existingApps = getApp(serverProfile, developerId);

    	        for (String app : entry.getValue()) {
    	        	String appName = getAppName(app);
    	        	if (appName == null) {
    	        		throw new IllegalArgumentException(
    	        			"App does not have a name.\n" + app + "\n");
    	        	}

            		if (doesDeveloperAppExist(serverProfile, developerId, appName)) {
                        switch (buildOption) {
                            case update:
                                logger.info("App \"" + appName + 
                                                        "\" exists. Updating.");
                                updateApp(serverProfile, developerId,
                                                        appName, app);
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
                                createApp(serverProfile, developerId, app);
                                break;
                        }
    	        	} else {
                        switch (buildOption) {
                            case create:
                            case sync:
                            case update:
                                logger.info("Creating App - " + appName);
                                createApp(serverProfile, developerId, app);
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
                                    String app)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createOrgConfig(profile, 
                                        "developers/" + developerId + "/apps",
                                         app);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("App create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateApp(ServerProfile profile,
                                    String developerId, 
                                    String appName, 
                                    String app)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.updateOrgConfig(profile, 
                                        "developers/" + developerId + "/apps", 
                                        appName,
                                        removeApiProductFromApp(profile, app));
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Update Success.");

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
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteOrgConfig(profile, 
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

    public static List getApp(ServerProfile profile, String developerId)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, 
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
    
    //Method to remove the apiProducts from App payload for config update option so that it does not create credentials everytime
    //https://github.com/apigee/apigee-config-maven-plugin/issues/17
    public static String removeApiProductFromApp(ServerProfile profile, String appPayload){
		if(profile.getIgnoreProductsForApp()) {
			logger.info("Ignoring the API Product config from App");
			JsonParser parser = new JsonParser();
			JsonElement jsonElement = parser.parse(appPayload);
			JsonObject appJsonObj = jsonElement.getAsJsonObject();
			appJsonObj.remove("apiProducts");
			return appJsonObj.toString();
		}else
			return appPayload; 	
	}
    
    public static boolean doesDeveloperAppExist(ServerProfile profile, String developerEmail, String appName)
            throws IOException {
        try {
        	logger.info("Checking if developerApp - " +appName + " exist");
        	RestUtil restUtil = new RestUtil(profile);
            HttpResponse response = restUtil.getOrgConfig(profile, "developers/"+developerEmail+"/apps/"+appName);
            if(response == null) 
            	return false;
        } catch (HttpResponseException e) {
            throw new IOException(e.getMessage());
        }

        return true;
    }	
}




