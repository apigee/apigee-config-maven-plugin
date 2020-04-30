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

import static java.lang.String.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.google.gson.JsonParseException;

/**                                                                                                                                     ¡¡
 * Goal to create API Specs in Apigee EDGE
 * scope: org
 *
 * @author saisaran.vaidyanathan
 * @goal specs
 * @phase verify
 */

public class SpecsMojo extends GatewayAbstractMojo
{
	static Logger logger = LoggerFactory.getLogger(SpecsMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class APISpec {
        @Key
        public String name;
        @Key
        public String file;
    }
    
    public static class Spec {
        private String name;
        private String id;
        private String folderId;
        
        public String getName() {
    		return name;
    	}

    	public void setName(String name) {
    		this.name = name;
    	}
    	
    	public String getId() {
    		return id;
    	}

    	public void setId(String id) {
    		this.id = id;
    	}
    	
    	public String getFolderId() {
    		return folderId;
    	}

    	public void setFolderId(String folderId) {
    		this.folderId = folderId;
    	}
    }
    
    public static class Specs {
    	private String homeFolderId;
        private List<String> specs;
        private Map<String, Spec> specsMap;
        
        public String getHomeFolderId() {
    		return homeFolderId;
    	}

    	public void setHomeFolderId(String homeFolderId) {
    		this.homeFolderId = homeFolderId;
    	}
    	
        public List<String> getSpecs() {
    		return specs;
    	}

    	public void setSpecs(List<String> specs) {
    		this.specs = specs;
    	}
    	
    	public Map<String, Spec> getSpecsMap() {
    		return specsMap;
    	}

    	public void setSpecsMap(Map<String, Spec> specsMap) {
    		this.specsMap = specsMap;
    	}
    }
	
	public SpecsMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Specs");
			logger.info("The APIs used in this goal are preliminary and subject to change without notice!!");
			logger.info(____ATTENTION_MARKER____);

			String options="";
			serverProfile = super.getProfile();			
			serverProfile.setAuthType("oauth"); //Spec API only supports OAuth
			serverProfile.setHostUrl("https://apigee.com"); //Spec API only supports OAuth
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

	protected APISpec getAPISpec(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			APISpec spec = gson.fromJson(payload, APISpec.class);
			return spec;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(List<String> specs) 
            throws MojoFailureException {
		try {
			List existingSpecs = null;
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create &&
                buildOption != OPTIONS.delete &&
                buildOption != OPTIONS.sync) {
				return;
			}

			logger.info("Retrieving existing Specs");
			Specs apiSpecs = getAPISpecs(serverProfile);
			existingSpecs = apiSpecs.getSpecs();

	        for (String spec : specs) {
	        	String specName = getAPISpec(spec).name;
	        	String specFilePath = getAPISpec(spec).file;
	        	if (specName == null) {
	        		throw new IllegalArgumentException(
	        			"API Spec does not have a name.\n" + spec + "\n");
	        	}

        		if (existingSpecs.contains(specName)) {
        			Spec s = apiSpecs.getSpecsMap().get(specName);
                    switch (buildOption) {
                        case update:
    						logger.info("API Spec \"" + specName + 
			           					"\" exists. Updating.");
	          				updateAPISpec(serverProfile, specName, s, specFilePath);
                            break;
                        case create:
        			        logger.info("API Spec \"" + specName + 
    									"\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("API Spec \"" + specName + 
                                        "\" already exists. Deleting.");
                            deleteAPISpec(serverProfile, specName, s);
                            break;
                        case sync:
                            logger.info("API Spec \"" + specName + 
                                        "\" already exists. Deleting and recreating.");
                            deleteAPISpec(serverProfile, specName, s);
                            logger.info("Creating API Spec - " + specName);
                            createAPISpec(serverProfile, specName, apiSpecs.getHomeFolderId(), specFilePath);
                            break;
	        		}
	        	} else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating API Spec - " + specName);
                            createAPISpec(serverProfile, specName, apiSpecs.getHomeFolderId(), specFilePath);
                            break;
                        case delete:
                            logger.info("API Spec \"" + specName + 
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

		Logger logger = LoggerFactory.getLogger(SpecsMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping API Spec (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			List specs = getOrgConfig(logger, "specs");
			if (specs == null || specs.size() == 0) {
				logger.info("No API Specs found.");
                return;
			}

			doUpdate(specs);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createAPISpec(ServerProfile profile, String specName, String folderId, String specFilePath)
            throws IOException {
    	String payload = format("{\n" + 
				"   \"name\": \"%s\",\n" + 
				"   \"folder\": %s,\n" + 
				"   \"kind\": \"Doc\"\n" + 
				"}", specName,
					 folderId);
        HttpResponse response = null;
        String id = null;
        String responsePayload = null;
        RestUtil restUtil = new RestUtil(profile);
        try {
        	response = restUtil.createAPISpec(profile, payload);
        	responsePayload = response.parseAsString();
            logger.debug(responsePayload);
            logger.info("Response " + response.getContentType() + "\n" + responsePayload);
            JSONParser parser = new JSONParser();
			JSONObject obj     = (JSONObject)parser.parse(responsePayload);
			id = (String) obj.get("id");
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("API Spec create error " + e.getMessage());
            throw new IOException(e.getMessage());
        } catch (ParseException e) {
        	 logger.error("API Spec parse error " + e.getMessage());
             throw new IOException(e.getMessage());
		}
        logger.info("Created Spec Id: " + id);
        logger.info("Uploading API Spec - " + specName);
        
        try {
        	response = restUtil.uploadAPISpec(profile, id, specFilePath);
        	responsePayload = response.parseAsString();
            logger.debug(responsePayload);
            logger.info("Response " + response.getContentType() + "\n" + responsePayload);
            if (response.isSuccessStatusCode())
            	logger.info("Upload Success.");
        }catch (HttpResponseException e) {
            logger.error("API Spec upload error " + e.getMessage());
            throw new IOException(e.getMessage());
        }
        
        return "";
    }

    public static String updateAPISpec(ServerProfile profile, 
                                        String specName, 
                                        Spec spec,
                                        String specFilePath) throws IOException {
        try {
        	RestUtil restUtil = new RestUtil(profile);
        	HttpResponse response = restUtil.uploadAPISpec(profile, spec.getId(), specFilePath);
        	String responsePayload = response.parseAsString();
            logger.debug(responsePayload);
            logger.info("Response " + response.getContentType() + "\n" + responsePayload);
            if (response.isSuccessStatusCode())
            	logger.info("Update Success.");
        }catch (HttpResponseException e) {
            logger.error("API Spec update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }
        return "";
    }

    public static String deleteAPISpec(ServerProfile profile,
                                            String specName, Spec spec)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteAPISpec(profile, spec.getId());
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("API Spec delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }
    
    /**
     * Fetch all the API Specs from the org
     * Set the list of spec names as list to check if already exists
     * Set the map for subsequent update/create/delete as the folderId, id is required
     * @param profile
     * @return
     * @throws IOException
     */
    public static Specs getAPISpecs(ServerProfile profile)
            throws IOException {
    	Specs specs = new Specs();
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getAllAPISpecs(profile);
        List<String> specList = null;
        Map<String, Spec> specMap = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            JSONParser parser = new JSONParser();
			JSONObject obj     = (JSONObject)parser.parse(payload);
			//homeFolder
			specs.setHomeFolderId((String) obj.get("id"));
			JSONArray contents = (JSONArray)obj.get("contents");
			specList = new ArrayList<String>(contents.size());
			specMap = new HashMap<String, Spec>(contents.size());
			for(Object content: contents){
			    if ( content instanceof JSONObject ) {
			    	Spec s = new Spec();
			    	s.setName(((JSONObject) content).get("name").toString());
			    	s.setId(((JSONObject) content).get("id").toString());
			    	s.setFolderId(((JSONObject) content).get("folderId").toString());
			    	specMap.put(s.getName(), s);
			    	specList.add(s.getName());
			    }
			}
			logger.debug(specList.toString());
			specs.setSpecs(specList);
			specs.setSpecsMap(specMap);
        } catch (ParseException pe){
            logger.error("Get API Spec parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get API Spec error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return specs;
    }	
}

