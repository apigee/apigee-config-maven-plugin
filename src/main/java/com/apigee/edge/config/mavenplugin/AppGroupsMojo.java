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
 * Goal to create AppGroups in Apigee
 * scope: org
 *
 * @author ssvaidyanathan
 * @goal appgroups
 * @phase install
 */

public class AppGroupsMojo extends GatewayAbstractMojo
{
	static Logger logger = LogManager.getLogger(AppGroupsMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class AppGroup {
        @Key
        public String name;
    }
	
	public AppGroupsMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee App Group");
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

	protected String getAppGroupName(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			AppGroup appGroup = gson.fromJson(payload, AppGroup.class);
			return appGroup.name;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(List<String> appGroups) 
            throws MojoFailureException {
		try {
			//List existingAppGroups = null;
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create &&
                buildOption != OPTIONS.delete &&
                buildOption != OPTIONS.sync) {
				return;
			}

	        for (String appGroup : appGroups) {
	        	String appGroupName = getAppGroupName(appGroup);
	        	if (appGroupName == null) {
	        		throw new IllegalArgumentException(
	        			"App Group does not have a name.\n" + appGroup + "\n");
	        	}

	        	if (doesAppGroupExist(serverProfile, appGroupName)) {
                    switch (buildOption) {
                        case update:
    						logger.info("App Group \"" + appGroupName + 
			           					"\" exists. Updating.");
	          				updateAppGroup(serverProfile, appGroupName, appGroup);
                            break;
                        case create:
        			        logger.info("App Group \"" + appGroupName + 
    									"\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("App Group \"" + appGroupName + 
                                        "\" already exists. Deleting.");
                            deleteAppGroup(serverProfile, appGroupName);
                            break;
                        case sync:
                            logger.info("App Group \"" + appGroupName + 
                                        "\" already exists. Deleting and recreating.");
                            deleteAppGroup(serverProfile, appGroupName);
                            logger.info("Creating App Group - " + appGroupName);
                            createAppGroup(serverProfile, appGroup);
                            break;
	        		}
	        	} else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating App Group - " + appGroupName);
                            createAppGroup(serverProfile, appGroup);
                            break;
                        case delete:
                            logger.info("App Group \"" + appGroupName + 
                                        "\" does not exist. Skipping.");
                            break;
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

		Logger logger = LogManager.getLogger(AppGroupsMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping App Groups (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			List appGroups = getOrgConfig(logger, "appGroups");
			if (appGroups == null || appGroups.size() == 0) {
				logger.info("No App Groups found.");
                return;
			}

			doUpdate(appGroups);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createAppGroup(ServerProfile profile, String appGroup)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createOrgConfig(profile, 
                                                         "appgroups",
                                                         appGroup);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("App Group create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateAppGroup(ServerProfile profile, 
                                        String appGroupName, 
                                        String appGroup)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.updateOrgConfig(profile, 
                                                        "appgroups", 
                                                        appGroupName,
                                                        appGroup);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("App Group update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteAppGroup(ServerProfile profile,
                                            String appGroupName)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteOrgConfig(profile, 
                                                        "appgroups", 
                                                        appGroupName);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("App Group delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getAppGroup(ServerProfile profile)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, 
                                                    "appgroups");
        if(response == null) return new ArrayList();
        JSONArray appGroups = new JSONArray();
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);
            JSONParser parser = new JSONParser();       
            JSONObject obj     = (JSONObject)parser.parse(payload);
            JSONArray appGroupsArray    = (JSONArray)obj.get("appGroups");
            for (int i = 0; appGroupsArray != null && i < appGroupsArray.size(); i++) {
             	 JSONObject a = (JSONObject) appGroupsArray.get(i);
             	appGroups.add(a.get("name"));
           }
        } catch (ParseException pe){
            logger.error("Get App Group parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get App Group error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return appGroups;
    }
    
    public static boolean doesAppGroupExist(ServerProfile profile, String appGroup)
            throws IOException {
        try {
        	RestUtil restUtil = new RestUtil(profile);
        	logger.info("Checking if App Group - " +appGroup + " exist");
            HttpResponse response = restUtil.getOrgConfig(profile, "appgroups/"+URLEncoder.encode(appGroup, "UTF-8"));
            if(response == null) 
            	return false;
        } catch (HttpResponseException e) {
            throw new IOException(e.getMessage());
        }

        return true;
    }
}




