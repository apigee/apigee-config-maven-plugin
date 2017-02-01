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

import com.apigee.edge.config.rest.RestUtil;
import com.apigee.edge.config.utils.ServerProfile;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Key;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import com.google.api.client.http.*;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**                                                                                                                                     ¡¡
 * Goal to create Developer in Apigee EDGE
 * scope: org
 *
 * @author madhan.sadasivam
 * @goal developers
 * @phase install
 */

public class DeveloperMojo extends GatewayAbstractMojo
{
	static Logger logger = LoggerFactory.getLogger(DeveloperMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class Developer {
        @Key
        public String email;
    }
	
	public DeveloperMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Developer");
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

	protected String getDeveloperName(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			Developer developer = gson.fromJson(payload, Developer.class);
			return developer.email;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(List<String> developers) 
            throws MojoFailureException {
		try {
			List existingDevelopers = null;
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create && 
                buildOption != OPTIONS.delete && 
                buildOption != OPTIONS.sync) {
				return;
			}

			logger.info("Retrieving existing Developers");
			existingDevelopers = getDeveloper(serverProfile);

	        for (String developer : developers) {
	        	String developerId = getDeveloperName(developer);
	        	if (developerId == null) {
	        		throw new IllegalArgumentException(
	        			"Developer does not have an id.\n" + developer + "\n");
	        	}

        		if (existingDevelopers.contains(developerId)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Developer \"" + developerId + 
                                                    "\" exists. Updating.");
                            updateDeveloper(serverProfile,
                                                    developerId, developer);
                            break;
                        case create:
                            logger.info("Developer \"" + developerId + 
                                                "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Developer \"" + developerId + 
                                    "\" already exists. Deleting.");
                            deleteDeveloper(serverProfile, developerId);
                            break;
                        case sync:
                            logger.info("Developer \"" + developerId + 
                                    "\" already exists. Deleting and recreating.");
                            deleteDeveloper(serverProfile, developerId);
                            logger.info("Creating Developer - " + developerId);
                            createDeveloper(serverProfile, developer);
                                break;
                    }
	        	} else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Developer - " + developerId);
                            createDeveloper(serverProfile, developer);
                            break;
                        case delete:
                            logger.info("Developer \"" + developerId + 
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
			getLog().info("Skipping");
			return;
		}

		Logger logger = LoggerFactory.getLogger(DeveloperMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Developers (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			List developers = getOrgConfig(logger, "developers");
			if (developers == null || developers.size() == 0) {
				logger.info("No Developers found.");
                return;
			}

			doUpdate(developers);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createDeveloper(ServerProfile profile, String developer)
            throws IOException {

        HttpResponse response = RestUtil.createOrgConfig(profile, 
                                                         "developers",
                                                         developer);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Developer create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateDeveloper(ServerProfile profile, 
                                        String developerId, 
                                        String developer)
            throws IOException {

        HttpResponse response = RestUtil.updateOrgConfig(profile, 
                                                        "developers", 
                                                        developerId,
                                                        developer);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("Developer update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteDeveloper(ServerProfile profile, 
                                        String developerId)
            throws IOException {

        HttpResponse response = RestUtil.deleteOrgConfig(profile, 
                                                        "developers", 
                                                        developerId);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Developer delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getDeveloper(ServerProfile profile)
            throws IOException {

        HttpResponse response = RestUtil.getOrgConfig(profile, "developers");
        if(response == null) return new ArrayList();
        JSONArray developers = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"developers\": " + payload + "}";

            JSONParser parser = new JSONParser();                
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            developers    = (JSONArray)obj1.get("developers");

        } catch (ParseException pe){
            logger.error("Get Developer parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Developer error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return developers;
    }	
}




