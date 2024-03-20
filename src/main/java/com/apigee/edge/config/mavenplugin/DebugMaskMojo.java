/**
 * Copyright (C) 2024 Google Inc
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**                                                                                                                                     ¡¡
 * Goal to create debug in Apigee X/hybrid.
 * scope: env
 *
 * @author ssvaidyanathan
 * @goal debugmasks
 * @phase install
 */

public class DebugMaskMojo extends GatewayAbstractMojo
{
	static Logger logger = LogManager.getLogger(DebugMaskMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

	public static class DebugMask {
        @Key
        public String name;
        @Key
        public Map<String, String> namespaces;
        @Key
        public Set<String> requestXPaths;
        @Key
        public Set<String> responseXPaths;
        @Key
        public Set<String> faultXPaths;
        @Key
        public Set<String> requestJSONPaths;
        @Key
        public Set<String> responseJSONPaths;
        @Key
        public Set<String> faultJSONPaths;
        @Key
        public Set<String> variables;
        
        public DebugMask union(DebugMask mask1, DebugMask mask2) {
    		DebugMask mask3 = new DebugMask();
    		mask3.namespaces = mergeMap(mask1.namespaces, mask2.namespaces);
    		mask3.requestXPaths = mergeSet(mask1.requestXPaths, mask2.requestXPaths);
    		mask3.responseXPaths = mergeSet(mask1.responseXPaths, mask2.responseXPaths);
    		mask3.faultXPaths = mergeSet(mask1.faultXPaths, mask2.faultXPaths);
    		mask3.requestJSONPaths =  mergeSet(mask1.requestJSONPaths, mask2.requestJSONPaths);
    		mask3.responseJSONPaths =  mergeSet(mask1.responseJSONPaths, mask2.responseJSONPaths);
    		mask3.faultJSONPaths =  mergeSet(mask1.faultJSONPaths, mask2.faultJSONPaths);
    		mask3.variables = mergeSet(mask1.variables, mask2.variables);
    		return mask3;
    	}
        
        public DebugMask diff(DebugMask mask1, DebugMask mask2) {
    		DebugMask mask3 = new DebugMask();
    		mask3.namespaces = diffMap(mask1.namespaces, mask2.namespaces);
    		mask3.requestXPaths = diffSet(mask1.requestXPaths, mask2.requestXPaths);
    		mask3.responseXPaths = diffSet(mask1.responseXPaths, mask2.responseXPaths);
    		mask3.faultXPaths = diffSet(mask1.faultXPaths, mask2.faultXPaths);
    		mask3.requestJSONPaths =  diffSet(mask1.requestJSONPaths, mask2.requestJSONPaths);
    		mask3.responseJSONPaths =  diffSet(mask1.responseJSONPaths, mask2.responseJSONPaths);
    		mask3.faultJSONPaths =  diffSet(mask1.faultJSONPaths, mask2.faultJSONPaths);
    		mask3.variables = diffSet(mask1.variables, mask2.variables);
    		return mask3;
    	}
    	
    	private static Set<String> mergeSet (Set<String> set1, Set<String> set2) {
    		if(set2!=null) {
    			if(set1!=null)
    				set1.addAll(set2);
    			else {
    				set1 = new HashSet<String>();
    				set1.addAll(set2);
    			}
    			return set1;
    		}
    		return set1;
    	}
    	
    	private static Set<String> diffSet (Set<String> set1, Set<String> set2) {
    		if(set2!=null) {
    			if(set1!=null)
    				set1.removeAll(set2);
    			else {
    				set1 = new HashSet<String>();
    				set1.removeAll(set2);
    			}
    			return set1;
    		}
    		return set1;
    	}
    	
    	private static Map<String, String> mergeMap (Map<String, String> map1, Map<String, String> map2) {
    		if(map2!=null) {
    			if(map1!=null)
    				map1.putAll(map2);
    			else {
    				map1 = new HashMap<String, String>();
    				map1.putAll(map2);
    			}
    			return map1;
    		}
    		return map1;
    	}
    	
    	private static Map<String, String> diffMap (Map<String, String> map1, Map<String, String> map2) {
    		if(map2!=null) {
    			if(map1!=null)
    				map1.entrySet().removeAll(map2.entrySet());
    			else {
    				map1 = new HashMap<String, String>();
    				map1.entrySet().removeAll(map2.entrySet());
    			}
    			return map1;
    		}
    		return map1;
    	}
    	
   
    	public static boolean isEmpty(DebugMask debugMask) {
    		if(debugMask!=null && (debugMask.namespaces==null || debugMask.namespaces.size()==0)
    				&& (debugMask.requestXPaths==null || debugMask.requestXPaths.size()==0)
    				&& (debugMask.responseXPaths==null || debugMask.responseXPaths.size()==0)
    				&& (debugMask.faultXPaths==null || debugMask.faultXPaths.size()==0)
    				&& (debugMask.requestJSONPaths==null || debugMask.requestJSONPaths.size()==0)
    				&& (debugMask.responseJSONPaths==null || debugMask.responseJSONPaths.size()==0)
    				&& (debugMask.faultJSONPaths==null || debugMask.faultJSONPaths.size()==0)
    				&& (debugMask.variables==null || debugMask.variables.size()==0))
    			return true;
    		else
    			return false;
    	}
    } 
	
	public DebugMaskMojo() {
		super();
	}
	
	public void init() throws MojoFailureException {

		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Debug Mask");
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

	protected DebugMask parseDebugMask(String payload) throws MojoFailureException {
		Gson gson = new Gson();
		try {
			DebugMask debugMask = gson.fromJson(payload, DebugMask.class);
			return debugMask;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(List<String> debugMasks) throws MojoFailureException {
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			DebugMask existingDebugMask = null;
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create && 
                buildOption != OPTIONS.delete && 
                buildOption != OPTIONS.sync) {
				return;
			}

			logger.info("Retrieving existing environment Debug Masks - " +
                                                serverProfile.getEnvironment());
			existingDebugMask = getExistingDebugMask(serverProfile);
			
			if(existingDebugMask!=null && DebugMask.isEmpty(existingDebugMask)) {
				//No DebugMask exist in Apigee, so just create the entire record passed
				switch (buildOption) {
	                case update:
	                case create:
	                case sync:
	                    logger.info("Debug Mark does not exist. Creating.");
	                    createDebugMask(serverProfile, debugMasks.get(0), "Create");
	                    break;
	                case delete:
	                    logger.info("Debug Mask does not exist. Skipping.");
	                    break;
				}
			}else {
				//DebugMask exist in Apigee, so need to manipulate the payload 
				switch (buildOption) {
					case create:
					case update:
					case delete:
						logger.info("---- ATTENTION ----");
						logger.info("WARNING: Create, Update, or Delete option is not allowed when a Debug Mask already exist!");
						logger.info("Existing DebugMask in the "+ serverProfile.getEnvironment() +" environment: ");
						logger.info(gson.toJson(existingDebugMask));
						logger.info("Use the existing DebugMask config and replace the config file. Add, update or delete entries to it and use the 'sync' option");
						logger.info("---- ATTENTION ----");
						//createDebugMask(serverProfile, new Gson().toJson(new DebugMask().union(existingDebugMask, parseDebugMask(debugMasks.get(0)))), "Create");
						break;
					case sync:
	                    logger.info("Syncing the Debug Mask.");
	                    syncDebugMask(serverProfile, debugMasks.get(0));
	                    break;
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

		Logger logger = LogManager.getLogger(DebugMaskMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Debug Mask (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

            List debugMasks = getEnvConfig(logger, "debugMasks");
			if (debugMasks == null || debugMasks.size() == 0) {
				logger.info(
                    "No debugMask config found.");
				return;
			}

			doUpdate(debugMasks);
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
	
	public static DebugMask getExistingDebugMask(ServerProfile profile)
            throws IOException {
		Gson gson = new Gson();
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getEnvConfig(profile, "debugmask");
        if(response == null) return null;
        DebugMask debugMask = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);
            debugMask = gson.fromJson(payload, DebugMask.class);
        } catch (JsonParseException jpe){
            logger.error("Get Debug Mask parse error " + jpe.getMessage());
            throw new IOException(jpe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Debug Mask error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return debugMask;
    }	
	
	public static String createDebugMask(ServerProfile profile, String debugMask, String operation)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
    	HttpResponse response;
        try {
        	response = restUtil.patchEnvConfig(profile, "debugmask", debugMask);
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info(operation + " Success.");

        } catch (HttpResponseException e) {
            logger.error("Debug Mask create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }
	
	public static String syncDebugMask(ServerProfile profile, String debugMask)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
    	HttpResponse response;
        try {
        	response = restUtil.patchEnvConfig(profile, "debugmask?replaceRepeatedFields=true", debugMask);
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Sync Success.");

        } catch (HttpResponseException e) {
            logger.error("Debug Mask create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }
	
}



