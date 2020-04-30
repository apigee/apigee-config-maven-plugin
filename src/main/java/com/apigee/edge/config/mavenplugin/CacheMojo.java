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
 * Goal to create cache in Apigee EDGE
 * scope: org
 *
 * @author madhan.sadasivam
 * @goal caches
 * @phase install
 */

public class CacheMojo extends GatewayAbstractMojo
{
	static Logger logger = LoggerFactory.getLogger(CacheMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class Cache {
        @Key
        public String name;
    }
	
	public CacheMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Cache");
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

	protected String getCacheName(String payload) throws MojoFailureException {
		Gson gson = new Gson();
		try {
			Cache cache = gson.fromJson(payload, Cache.class);
			return cache.name;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	/**
	 * create Cache values
	 */
	protected void doUpdate(List<String> caches) throws MojoFailureException {
		try {
			List existingCaches = null;
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create && 
                buildOption != OPTIONS.delete && 
                buildOption != OPTIONS.sync) {
				return;
			}

			logger.info("Retrieving existing environment caches - " +
                                                serverProfile.getEnvironment());
			existingCaches = getCache(serverProfile);

	        for (String cache : caches) {
	        	String cacheName = getCacheName(cache);
	        	if (cacheName == null) {
	        		throw new IllegalArgumentException(
	        			"Cache does not have a name.\n" + cache + "\n");
	        	}

        		if (existingCaches.contains(cacheName)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Cache \"" + cacheName + 
                                                    "\" exists. Updating.");
                            updateCache(serverProfile, cacheName, cache);
                            break;
                        case create:
                            logger.info("Cache \"" + cacheName + 
                                                "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Cache \"" + cacheName + 
                                                "\" already exists. Deleting.");
                            deleteCache(serverProfile, cacheName);
                            break;
                        case sync:
                            logger.info("Cache \"" + cacheName + 
                                                "\" already exists. Deleting and recreating.");
                            deleteCache(serverProfile, cacheName);
                            logger.info("Creating Cache - " + cacheName);
                            createCache(serverProfile, cache);
                            break;
                    }
	        	} else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Cache - " + cacheName);
                            createCache(serverProfile, cache);
                            break;
                        case delete:
                            logger.info("Cache \"" + cacheName + 
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

		Logger logger = LoggerFactory.getLogger(CacheMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Caches (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			List caches = getEnvConfig(logger, "caches");
			if (caches == null || caches.size() == 0) {
				logger.info("No cache config found.");
                return;
			}

			doUpdate(caches);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createCache(ServerProfile profile, String cache)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createEnvConfig(profile, "caches", cache);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Cache create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateCache(ServerProfile profile, 
                                        String cacheName, 
                                        String cache)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.updateEnvConfig(profile, "caches", 
                                                    cacheName, cache);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("Cache update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteCache(ServerProfile profile, 
                                        String cacheName)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteEnvConfig(profile, "caches", 
                                                    cacheName);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Cache delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getCache(ServerProfile profile)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getEnvConfig(profile, "caches");
        if(response == null) return new ArrayList();
        JSONArray caches = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"caches\": " + payload + "}";

            JSONParser parser = new JSONParser();                
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            caches    = (JSONArray)obj1.get("caches");

        } catch (ParseException pe){
            logger.error("Get Cache parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Cache error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return caches;
    }	
}




