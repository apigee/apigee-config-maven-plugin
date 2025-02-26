/**
 * Copyright (C) 2025 Google LLC
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
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.apigee.edge.config.rest.RestUtil;
import com.apigee.edge.config.utils.ServerProfile;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.util.Key;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;


/**                                                                                                                                     ¡¡
 * Goal to manage Spaces in Apigee
 * scope: org
 *
 * @author ssvaidyanathan
 * @goal spaces
 * @phase install
 */
public class SpacesMojo extends GatewayAbstractMojo {
	static Logger logger = LogManager.getLogger(SpacesMojo.class);
	public static final String ____ATTENTION_MARKER____ = "************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

	public static class Space {
		@Key
		public String name;
		@Key
		public String displayName;
	}

	public SpacesMojo() {
		super();

	}

	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Spaces");
			logger.info(____ATTENTION_MARKER____);

			String options = "";
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
	
	protected String getSpaceName(String payload) throws MojoFailureException {
		Gson gson = new Gson();
		try {
			Space space = gson.fromJson(payload, Space.class);
			return space.name;
		} catch (JsonParseException e) {
			throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(List<String> spaces) throws MojoFailureException {
		try {
			if (buildOption != OPTIONS.update && buildOption != OPTIONS.create && buildOption != OPTIONS.delete
					&& buildOption != OPTIONS.sync) {
				return;
			}
			for (String space : spaces) {
				String spaceName = getSpaceName(space);
				if (spaceName == null) {
					throw new IllegalArgumentException("Space does not have a name.\n" + space + "\n");
				}
				
				if (doesSpaceExist(serverProfile, spaceName)) {
						switch (buildOption) {
	                    case update:
							logger.info("Space \"" + spaceName + 
			           					"\" exists. Updating.");
	          				updateSpace(serverProfile, spaceName, space);
	                        break;
	                    case create:
	    			        logger.info("Space \"" + spaceName + 
										"\" already exists. Skipping.");
	                        break;
	                    case delete:
	                        logger.info("Space \"" + spaceName + 
	                                    "\" already exists. Deleting.");
	                        deleteSpace(serverProfile, spaceName);
	                        break;
	                    case sync:
	                        logger.info("Space \"" + spaceName + 
	                                    "\" already exists. Deleting and recreating.");
	                        deleteSpace(serverProfile, spaceName);
	                        logger.info("Creating Space - " + spaceName);
	                        createSpace(serverProfile, space);
	                        break;
	        		}
				} else {
                    switch (buildOption) {
                    case create:
                    case sync:
                    case update:
                        logger.info("Creating Space - " + spaceName);
                        createSpace(serverProfile, space);
                        break;
                    case delete:
                        logger.info("Space \"" + spaceName + 
                                    "\" does not exist. Skipping.");
                        break;
                    }
	        	}
			}

		} catch (IOException e) {
			throw new MojoFailureException("Apigee network call error " + e.getMessage());
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

		Logger logger = LogManager.getLogger(SpacesMojo.class);

		try {

			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Spaces (default action)");
				return;
			}

			if (serverProfile.getEnvironment() == null) {
				throw new MojoExecutionException("Apigee environment not found in profile");
			}

			List spaces = getOrgConfig(logger, "spaces");
			if (spaces == null || spaces.size() == 0) {
				logger.info("No Spaces found.");
				return;
			}

			doUpdate(spaces);

		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

	/***************************************************************************
	 * REST call wrappers
	 **/
	public static String createSpace(ServerProfile profile, String space) throws IOException {
		RestUtil restUtil = new RestUtil(profile);
		HttpResponse response = restUtil.createOrgConfig(profile, "spaces", space);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Create Success.");

		} catch (HttpResponseException e) {
			logger.error("Space create error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}

	public static String updateSpace(ServerProfile profile, String spaceName, String space) throws IOException {
		RestUtil restUtil = new RestUtil(profile);
		HttpResponse response = restUtil.patchOrgConfig(profile, "spaces", spaceName, space);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Update Success.");

		} catch (HttpResponseException e) {
			logger.error("Space update error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}

	public static String deleteSpace(ServerProfile profile, String spaceName) throws IOException {
		RestUtil restUtil = new RestUtil(profile);
		HttpResponse response = restUtil.deleteOrgConfig(profile, "spaces", spaceName);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Delete Success.");

		} catch (HttpResponseException e) {
			logger.error("Space delete error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}

	public static boolean doesSpaceExist(ServerProfile profile, String space)
            throws IOException {
        try {
        	RestUtil restUtil = new RestUtil(profile);
        	logger.info("Checking if Space - " +space + " exist");
            HttpResponse response = restUtil.getOrgConfig(profile, "spaces/"+URLEncoder.encode(space, "UTF-8"));
            if(response == null) 
            	return false;
        } catch (HttpResponseException e) {
            throw new IOException(e.getMessage());
        }

        return true;
    }
}
