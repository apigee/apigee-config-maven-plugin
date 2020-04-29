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
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.api.client.http.*;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**                                                                                                                                     ¡¡
 * Goal to create maskconfigs in Apigee EDGE.
 * scope: org, api
 *
 * @author madhan.sadasivam
 * @goal maskconfigs
 * @phase install
 */

public class MaskConfigMojo extends GatewayAbstractMojo
{
	static Logger logger = LoggerFactory.getLogger(MaskConfigMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class MaskConfig {
        @Key
        public String name;
    }
	
	public MaskConfigMojo() {
		super();
	}
	
	public void init() throws MojoFailureException {

		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Mask Config");
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

	protected String getMaskConfigName(String payload) throws MojoFailureException {
		Gson gson = new Gson();
		try {
			MaskConfig maskConfig = gson.fromJson(payload, MaskConfig.class);
			return maskConfig.name;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

    protected void doOrgUpdate(List<String> masks) throws MojoFailureException {
        try {
            List existingMasks = getOrgMaskConfig(serverProfile);
            if (buildOption != OPTIONS.update && 
                buildOption != OPTIONS.create && 
                buildOption != OPTIONS.delete && 
                buildOption != OPTIONS.sync) {
                return;
            }

            for (String mask : masks) {
                String maskName = getMaskConfigName(mask);
                if (maskName == null) {
                    throw new IllegalArgumentException(
                       "Mask Config does not have a name.\n" + mask + "\n");
                }

                if (existingMasks.contains(maskName)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Org Mask Config \"" + maskName + 
                                                    "\" exists. Updating.");
                            updateOrgMaskConfig(serverProfile, maskName, mask);
                            break;
                        case create:
                            logger.info("Org Mask Config \"" + maskName + 
                                                "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Org Mask Config \"" + maskName + 
                                "\" already exists. Deleting.");
                            deleteOrgMaskConfig(serverProfile, maskName);
                            break;
                        case sync:
                            logger.info("Org Mask Config \"" + maskName + 
                                "\" already exists. Deleting and recreating.");
                            deleteOrgMaskConfig(serverProfile, maskName);
                            logger.info("Creating Org Mask Config - " + maskName);
                            createOrgMaskConfig(serverProfile, mask);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Org Mask Config - " + maskName);
                            createOrgMaskConfig(serverProfile, mask);
                            break;
                        case delete:
                            logger.info("Org Mask Config \"" + maskName + 
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

	protected void doAPIUpdate(String api, List<String> masks)
                                                 throws MojoFailureException {
		try {
			List existingMasks = getAPIMaskConfig(serverProfile, api);
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create && 
                buildOption != OPTIONS.delete && 
                buildOption != OPTIONS.sync) {
				return;
			}

	        for (String mask : masks) {
	        	String maskName = getMaskConfigName(mask);
	        	if (maskName == null) {
	        		throw new IllegalArgumentException(
	        		   "Mask Config does not have a name.\n" + mask + "\n");
	        	}

        		if (existingMasks.contains(maskName)) {
                    switch (buildOption) {
                        case update:
                            logger.info("API Mask Config \"" + maskName + 
                                                    "\" exists. Updating.");
                            updateAPIMaskConfig(serverProfile, api, maskName, mask);
                            break;
                        case create:
                            logger.info("API Mask Config \"" + maskName + 
                                                "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("API Mask Config \"" + maskName + 
                                            "\" already exists. Deleting.");
                            deleteAPIMaskConfig(serverProfile, api, maskName);
                            break;
                        case sync:
                            logger.info("API Mask Config \"" + maskName + 
                                            "\" already exists. Deleting and recreating.");
                            deleteAPIMaskConfig(serverProfile, api, maskName);
                            logger.info("Creating API Mask Config - " + maskName);
                            createAPIMaskConfig(serverProfile, api, mask);
                            break;
                    }
	        	} else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating API Mask Config - " + maskName);
                            createAPIMaskConfig(serverProfile, api, mask);
                            break;
                        case delete:
                            logger.info("API Mask Config \"" + maskName + 
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

		Logger logger = LoggerFactory.getLogger(MaskConfigMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Mask Config (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

            /* Org scoped Masks */
			List maskConfigs = getOrgConfig(logger, "maskconfigs");
			if (maskConfigs == null || maskConfigs.size() == 0) {
				logger.info("No org scoped Mask config found.");
			} else {
                doOrgUpdate(maskConfigs);
            }

            /* API scoped Masks */
            Set<String> apis = getAPIList(logger);
            if (apis == null || apis.size() == 0) {
                logger.info("No API scoped Mask config found in edge.json or config file");
                return;
            }

            for (String api : apis) {
                maskConfigs = getAPIConfig(logger, "maskconfigs", api);
                if (maskConfigs == null || maskConfigs.size() == 0) {
                    logger.info(
                        "No API scoped Mask config found in edge.json or config file");
                } else {
                    doAPIUpdate(api, maskConfigs);
                }
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
    public static String createOrgMaskConfig(ServerProfile profile, String mask)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createOrgConfig(profile, 
                                                            "maskconfigs", 
                                                            mask);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Mask Config create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateOrgMaskConfig(ServerProfile profile, 
                                        String maskEntry, 
                                        String mask)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.updateOrgConfig(profile, 
                                                            "maskconfigs", 
                                                            maskEntry,
                                                            mask);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("Mask Config update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteOrgMaskConfig(ServerProfile profile, String maskEntry)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteOrgConfig(profile, 
                                                            "maskconfigs", 
                                                            maskEntry);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Mask Config delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getOrgMaskConfig(ServerProfile profile)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, "maskconfigs");
        if(response == null) return new ArrayList();
        JSONArray masks = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"masks\": " + payload + "}";

            JSONParser parser = new JSONParser();                
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            masks    = (JSONArray)obj1.get("masks");

        } catch (ParseException pe){
            logger.error("Get Mask Config parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Mask Config error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return masks;
    }   

    public static String createAPIMaskConfig(ServerProfile profile, 
                                        String api,
                                        String mask)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createAPIConfig(profile, 
                                                            api,
                                                            "maskconfigs", 
                                                            mask);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Mask Config create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateAPIMaskConfig(ServerProfile profile, 
                                        String api,
                                        String maskEntry, 
                                        String mask)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.updateAPIConfig(profile, 
                                                            api,
                                                            "maskconfigs", 
                                                            maskEntry,
                                                            mask);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("Mask Config update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteAPIMaskConfig(ServerProfile profile, 
                                        String api,
                                        String maskEntry)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteAPIConfig(profile, 
                                                            api,
                                                            "maskconfigs", 
                                                            maskEntry);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Mask Config delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getAPIMaskConfig(ServerProfile profile, String api)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getAPIConfig(profile, api, 
                                                        "maskconfigs");
        if(response == null) return new ArrayList();
        JSONArray masks = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"masks\": " + payload + "}";

            JSONParser parser = new JSONParser();                
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            masks    = (JSONArray)obj1.get("masks");

        } catch (ParseException pe){
            logger.error("Get Mask Config parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Mask Config error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return masks;
    }   

}




