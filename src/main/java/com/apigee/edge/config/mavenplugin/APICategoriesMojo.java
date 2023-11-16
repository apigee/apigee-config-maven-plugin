/**
 * Copyright 2023 Google LLC
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
import java.util.HashMap;
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
 * Goal to create API Categories in Apigee
 * scope: org
 *
 * @author ssvaidyanathan
 * @goal apicategories
 * @phase install
 */

public class APICategoriesMojo extends GatewayAbstractMojo
{
	static Logger logger = LogManager.getLogger(APICategoriesMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;
	
	public APICategoriesMojo() {
		super();

	}
	
	public void init() throws MojoExecutionException, MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Portal API Categories");
			logger.info(____ATTENTION_MARKER____);

			String options="";
			serverProfile = super.getProfile();			
	
			options = super.getOptions();
			if (options != null) {
				buildOption = OPTIONS.valueOf(options);
			}
			logger.debug("Build option " + buildOption.name());
			logger.debug("Base dir " + super.getBaseDirectoryPath());
			if (serverProfile.getPortalSiteId() == null) {
		        throw new MojoExecutionException(
		          "Portal Site ID not found in profile");
		      }
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Invalid apigee.option provided");
		} catch (RuntimeException e) {
			throw e;
		}

	}

	protected void doUpdate(List<String> categories) 
            throws MojoFailureException {
		try {
			Map<String, String> existingCategories = null;
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create &&
                buildOption != OPTIONS.delete &&
                buildOption != OPTIONS.sync) {
				return;
			}
			logger.info("Retrieving existing API Categories - " +
                    serverProfile.getEnvironment());
			existingCategories = getCategories(serverProfile);

	        for (String category : categories) {
	        	if (existingCategories != null && existingCategories.keySet()!=null 
	        			&& existingCategories.keySet().contains(category)) {
                    switch (buildOption) {
                        case update:
                        	logger.info("API Category \"" + category + 
									"\" already exists. Skipping.");
                        	break;
                        case create:
        			        logger.info("API Category \"" + category + 
    									"\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("API Category \"" + category + 
                                        "\" already exists. Deleting.");
                            deleteAPICategory(serverProfile, existingCategories.get(category));
                            break;
                        case sync:
                            logger.info("API Category \"" + category + 
                                        "\" already exists. Deleting and recreating.");
                            deleteAPICategory(serverProfile, existingCategories.get(category));
                            logger.info("Creating API Category - " + category);
                            createAPICategory(serverProfile, category);
                            break;
	        		}
	        	} else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating API Category - " + category);
                            createAPICategory(serverProfile, category);
                            break;
                        case delete:
                            logger.info("API Category \"" + category + 
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

		Logger logger = LogManager.getLogger(APICategoriesMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping API Categories (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			List categories = getOrgConfig(logger, "apiCategories");
			if (categories == null || categories.size() == 0) {
				logger.info("No API Categories found.");
                return;
			}
			doUpdate(categories);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createAPICategory(ServerProfile profile, String category)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
    	String payload = "{\"name\": \""+category+"\"}";
        HttpResponse response = restUtil.createOrgConfig(profile, 
        												"sites/"+profile.getPortalSiteId()+"/apicategories",
                                                         payload);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("API Category create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteAPICategory(ServerProfile profile,
                                            String categoryId)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteOrgConfig(profile, 
                                                        "sites/"+profile.getPortalSiteId()+"/apicategories", 
                                                        categoryId);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("API Category delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }
    
    public static Map<String, String> getCategories(ServerProfile profile)
            throws IOException {
    	Map<String, String> categoryMap = new HashMap<String, String>();
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, "sites/"+profile.getPortalSiteId()+"/apicategories");
        if(response == null) return categoryMap;
        JSONArray categories = null;
        try {
            logger.debug("output " + response.getContentType());
            String payload = response.parseAsString();
            logger.debug(payload);

            JSONParser parser = new JSONParser();                
            JSONObject obj    = (JSONObject)parser.parse(payload);
            categories    = (JSONArray)obj.get("data");
            for (int i = 0; categories != null && i < categories.size(); i++) {
           	 JSONObject a = (JSONObject) categories.get(i);
           	 categoryMap.put((String)a.get("name"), (String)a.get("id"));
           }
        } catch (ParseException pe){
            logger.error("Get Categories parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Categories error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return categoryMap;
    }	

}




