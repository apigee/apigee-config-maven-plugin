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
 * Goal to create Resource Files in Apigee EDGE.
 * scope: env
 *
 * @author saisaran.vaidyanathan
 * @goal resourcefiles
 * @phase install
 */

public class ResourceFileMojo extends GatewayAbstractMojo
{
	static Logger logger = LoggerFactory.getLogger(ResourceFileMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class ResourceFile {
        @Key
        public String name;
        @Key
        public String type;
        @Key
        public String file;
        @Key
        public String revision;
    }
	
	public ResourceFileMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {

		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Resource File");
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

	protected ResourceFile getResourceFile(String payload) throws MojoFailureException {
		Gson gson = new Gson();
		try {
			ResourceFile resourceFile = gson.fromJson(payload, ResourceFile.class);
			return resourceFile;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

    protected void doEnvUpdate(List<String> resourcefiles)
                                                 throws MojoFailureException {
        try {
            List existingResourcefile = getExistingResourceFile(serverProfile);
            if (buildOption != OPTIONS.update && 
                buildOption != OPTIONS.create && 
                buildOption != OPTIONS.delete && 
                buildOption != OPTIONS.sync) {
                return;
            }

            for (String resourcefile : resourcefiles) {
                String resourcefileName = getResourceFile(resourcefile).name;
                String resourcefileType = getResourceFile(resourcefile).type;
                String resourcefilePath = getResourceFile(resourcefile).file;
                if (resourcefileName == null) {
                    throw new IllegalArgumentException(
                       "resourcefile does not have a name.\n" + resourcefile + "\n");
                }

                if (existingResourcefile.contains(resourcefileName+"_"+resourcefileType)) {
                    switch (buildOption) {
                    case update:
                        logger.info("Env Resource File \"" + resourcefileName + 
                                                "\" exists. Updating.");
                        updateEnvResourceFile(serverProfile, resourcefileType, resourcefileName, resourcefilePath);
                        break;
                    case create:
                        logger.info("Env Resource File \"" + resourcefileName + " of type "+ resourcefileType+
                                            "\" already exists. Skipping.");
                        break;
                    case delete:
                        logger.info("Env Resource File \"" + resourcefileName + " of type "+ resourcefileType+ 
                            "\" already exists. Deleting.");
                        deleteEnvResourceFile(serverProfile, resourcefileType, resourcefileName);
                        break;
                    case sync:
                        logger.info("Env Resource File \"" + resourcefileName + " of type "+ resourcefileType+
                            "\" already exists. Deleting and recreating.");
                        deleteEnvResourceFile(serverProfile, resourcefileType, resourcefileName);
                        logger.info("Creating Env Resource File - " + resourcefileName);
                        createEnvResourceFile(serverProfile, resourcefileType, resourcefileName, resourcefilePath);
                        break;
                    }
                } else {
                    switch (buildOption) {
	                    case create:
	                    case sync:
	                    case update:
	                        logger.info("Creating Env Resource File - " + resourcefileName + " of type "+ resourcefileType);
	                        createEnvResourceFile(serverProfile, resourcefileType, resourcefileName, resourcefilePath);
	                        break;
	                    case delete:
	                        logger.info("Env Resource File \"" + resourcefileName + " of type "+ resourcefileType+ 
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

		Logger logger = LoggerFactory.getLogger(ResourceFileMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Resource File (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

            /* env scoped resourcefiles */
			List resourcefiles = getEnvConfig(logger, "resourcefiles");
            if (resourcefiles == null || resourcefiles.size() == 0) {
                logger.info("No env scoped resourcefiles config found.");
            } else {
                doEnvUpdate(resourcefiles);
            }
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/

	public static String createEnvResourceFile(ServerProfile profile, String resourcefileType, String resourcefileName,
			String resourceFilePath) throws IOException {

		HttpResponse response = RestUtil.createEnvConfigUpload(profile,
				"resourcefiles" + "?type=" + resourcefileType + "&name=" + resourcefileName, resourceFilePath);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Create Success.");

		} catch (HttpResponseException e) {
			logger.error("resourcefile create error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}

	public static String updateEnvResourceFile(ServerProfile profile, String resourcefileType, String resourcefileName,
			String resourcefilePath) throws IOException {

		HttpResponse response = RestUtil.updateEnvConfigUpload(profile, "resourcefiles",
				resourcefileType + "/" + resourcefileName, resourcefilePath);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Update Success.");

		} catch (HttpResponseException e) {
			logger.error("resourceFile update error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}

	public static String deleteEnvResourceFile(ServerProfile profile, String resourcefileType, String resourcefileName)
			throws IOException {

		HttpResponse response = RestUtil.deleteEnvResourceFileConfig(profile, "resourcefiles",
				resourcefileType + "/" + resourcefileName);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Delete Success.");

		} catch (HttpResponseException e) {
			logger.error("resourceFile delete error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}
	
	public static List getExistingResourceFile(ServerProfile profile) throws IOException {

		HttpResponse response = RestUtil.getEnvConfig(profile, "resourcefiles");
		if (response == null)
			return new ArrayList();
		JSONArray resourcefilesArr = null;
		List resourcefiles = new ArrayList();
		;
		try {
			logger.debug("output " + response.getContentType());
			// response can be read only once
			String payload = response.parseAsString();
			logger.debug(payload);
			JSONParser parser = new JSONParser();
			JSONObject obj1 = (JSONObject) parser.parse(payload);
			resourcefilesArr = (JSONArray) obj1.get("resourceFile");
			if (resourcefilesArr != null && resourcefilesArr.size() > 0) {
				for (int i = 0; i < resourcefilesArr.size(); i++) {
					JSONObject resourcefile = (JSONObject) resourcefilesArr.get(i);
					resourcefiles.add(resourcefile.get("name") + "_" + resourcefile.get("type"));
				}
			}
		} catch (ParseException pe) {
			logger.error("Get resourcefiles parse error " + pe.getMessage());
			throw new IOException(pe.getMessage());
		} catch (HttpResponseException e) {
			logger.error("Get resourcefiles error " + e.getMessage());
			throw new IOException(e.getMessage());
		}
		return resourcefiles;
	}  
	
}




