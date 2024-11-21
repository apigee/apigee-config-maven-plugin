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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * Goal to manage Rate plans in Apigees
 * scope: org
 *
 * @author ssvaidyanathan
 * @goal rateplans
 * @phase install
 */

public class RatePlansMojo extends GatewayAbstractMojo
{
	static Logger logger = LogManager.getLogger(RatePlansMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class RatePlan {
        @Key
        public String name;
        @Key
        public String apiproduct;
    }
	
	public RatePlansMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Rate Plan");
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
	
	public static String getRatePlanProductName(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			RatePlan ratePlan = gson.fromJson(payload, RatePlan.class);
			return ratePlan.apiproduct;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(List<String> ratePlans) 
            throws MojoFailureException {
		try {
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create && 
                buildOption != OPTIONS.delete && 
                buildOption != OPTIONS.sync) {
				return;
			}
			if (buildOption == OPTIONS.update) {
				logger.info("update option for Rate Plan is not supported");
				return;
			}
			
			if (buildOption == OPTIONS.sync) {
				syncRatePlan(serverProfile, ratePlans);
				return;
			}

	        for (String ratePlan : ratePlans) {
	        	String apiProductName = getRatePlanProductName(ratePlan);
                switch (buildOption) {
                    case create:
                    	createRatePlan(serverProfile, apiProductName, ratePlan);
                        break;
                    case delete:
                        logger.info("Deleting all Rate Plans under API Product: "+ apiProductName);
                        deleteRatePlan(serverProfile, apiProductName);
                        break;
                    case sync:
                    	logger.info("Deleting all Rate Plans under API Product: "+ apiProductName);
                        deleteRatePlan(serverProfile, apiProductName);
                        logger.info("Creating Rate Plan");
                        createRatePlan(serverProfile, apiProductName, ratePlan);
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

		Logger logger = LogManager.getLogger(RatePlansMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Rate Plans (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			List rateplans = getOrgConfig(logger, "ratePlans");
			if (rateplans == null || rateplans.size() == 0) {
				logger.info("No Rate Plans found.");
                return;
			}

			doUpdate(rateplans);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createRatePlan(ServerProfile profile, String apiProduct, String ratePlan)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createOrgConfig(profile, 
                                                         "apiproducts", apiProduct,
                                                         "rateplans", ratePlan);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Rate Plan create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static void deleteRatePlan(ServerProfile profile, String apiProduct)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
    	List<String> ratePlans = getAllRatePlans(profile, apiProduct);
    	for (String ratePlanName : ratePlans) {
    		try {
    			logger.info("Deleting Rate plan: "+ ratePlanName);
    			HttpResponse response = restUtil.deleteOrgConfig(profile, 
    					"apiproducts", apiProduct,
    					"rateplans", ratePlanName);
    			logger.info("Response " + response.getContentType() + "\n" +
                        response.parseAsString());
    			if (response.isSuccessStatusCode())
                    logger.info("Delete Success.");
    		}catch (HttpResponseException e) {
                logger.error("Developer delete error " + e.getMessage());
                throw new IOException(e.getMessage());
            }
    		
		}
    }	
    
    public static void syncRatePlan(ServerProfile profile, List<String> ratePlans)
            throws IOException, MojoFailureException {
    	if(ratePlans == null || ratePlans.size()==0)
    		return;
    	//extract API Products
    	Set<String> products = new HashSet<String>(ratePlans.size());
    	for (String ratePlan : ratePlans) {
    		String apiProductName = getRatePlanProductName(ratePlan);
    		products.add(apiProductName);
		}
    	//delete all rate plans for each product
    	for (String product : products) {
    		deleteRatePlan(profile, product);
		}
    	//create rate plans for each product
    	for (String ratePlan : ratePlans) {
    		String apiProductName = getRatePlanProductName(ratePlan);
    		createRatePlan(profile, apiProductName, ratePlan);
		}
    }
    
    public static List<String> getAllRatePlans(ServerProfile profile, String apiProduct)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, 
        												"apiproducts", apiProduct,
        												"rateplans");
        if(response == null) return new ArrayList();
        JSONArray ratePlans = new JSONArray();
        try {
        	String payload = response.parseAsString();
        	JSONParser parser = new JSONParser();
        	JSONObject obj     = (JSONObject)parser.parse(payload);
            JSONArray ratePlansArray    = (JSONArray)obj.get("ratePlans");
            for (int i = 0; ratePlansArray != null && i < ratePlansArray.size(); i++) {
            	 JSONObject a = (JSONObject) ratePlansArray.get(i);
            	 ratePlans.add(a.get("name"));
          }
        }catch (ParseException pe){
            logger.error("Get Rate Plan parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Rate Plan error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return ratePlans;
    }
}




