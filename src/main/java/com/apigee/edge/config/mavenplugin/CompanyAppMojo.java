/**
 * Copyright 2021 Google LLC
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**                                                                                                                                     ¡¡
 * Goal to create CompanyApps in Apigee EDGE
 * scope: org
 *
 * @author ssvaidyanathan
 * @goal companyapps
 * @phase install
 */

public class CompanyAppMojo extends GatewayAbstractMojo
{
	static Logger logger = LoggerFactory.getLogger(CompanyAppMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class CompanyApp {
        @Key
        public String name;
    }
	
	public CompanyAppMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Company App");
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

	protected String getCompanyAppName(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			CompanyApp companyApp = gson.fromJson(payload, CompanyApp.class);
			return companyApp.name;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(Map<String, List<String>> companyApps) 
            throws MojoFailureException {
		try {
			List existingCompanyApps = null;
			if (buildOption != OPTIONS.update && 
                buildOption != OPTIONS.create &&
				buildOption != OPTIONS.delete &&
                buildOption != OPTIONS.sync) {
				return;
			}

            for (Map.Entry<String, List<String>> entry : companyApps.entrySet()) {

            	logger.info("Retrieving Company Apps of " + entry.getKey());
                String company = URLEncoder.encode(entry.getKey(), "UTF-8");
                existingCompanyApps = getCompanyApp(serverProfile, company);

    	        for (String companyApp : entry.getValue()) {
    	        	String companyAppName = getCompanyAppName(companyApp);
    	        	if (companyAppName == null) {
    	        		throw new IllegalArgumentException(
    	        			"Company App does not have a name.\n" + companyApp + "\n");
    	        	}

            		if (existingCompanyApps.contains(companyAppName)) {
                        switch (buildOption) {
                            case update:
                                logger.info("Company App \"" + companyAppName + 
                                                        "\" exists. Updating.");
                                updateCompanyApp(serverProfile, company,
                                                        companyAppName, companyApp);
                                break;
                            case create:
                                logger.info("Company App \"" + companyAppName + 
                                                "\" already exists. Skipping.");
                                break;
                            case delete:
                                logger.info("Company App \"" + companyAppName + 
                                                "\" already exists. Deleting.");
                                deleteCompanyApp(serverProfile, company, companyAppName);
                                break;
                            case sync:
                                logger.info("Company App \"" + companyAppName + 
                                                "\" already exists. Deleting and recreating.");
                                deleteCompanyApp(serverProfile, company, companyAppName);
                                logger.info("Creating Company App - " + companyAppName);
                                createCompanyApp(serverProfile, company, companyApp);
                                break;
                        }
    	        	} else {
                        switch (buildOption) {
                            case create:
                            case sync:
                            case update:
                                logger.info("Creating Company App - " + companyAppName);
                                createCompanyApp(serverProfile, company, companyApp);
                                break;
                            case delete:
                                logger.info("Company App \"" + companyAppName + 
                                                "\" does not exist. Skipping.");
                                break;
                        }
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

		Logger logger = LoggerFactory.getLogger(CompanyAppMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Company Apps (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			Map<String, List<String>> companyApps = getOrgConfigWithId(logger, "companyApps");
			if (companyApps == null || companyApps.size() == 0) {
				logger.info("No Company apps found.");
                return;
			}

            logger.debug(companyApps.toString());
			doUpdate(companyApps);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createCompanyApp(ServerProfile profile, 
                                    String company,
                                    String companyApp)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createOrgConfig(profile, 
                                        "companies/" + company + "/apps",
                                        companyApp);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Company App create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateCompanyApp(ServerProfile profile,
                                    String company, 
                                    String companyAppName, 
                                    String companyApp)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.updateOrgConfig(profile, 
                                        "companies/" + company + "/apps", 
                                        companyAppName,
                                        removeApiProductFromApp(profile,companyApp));
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("Company App update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteCompanyApp(ServerProfile profile,
                                    String company, 
                                    String companyAppName)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteOrgConfig(profile, 
                                        "companies/" + company + "/apps", 
                                        companyAppName);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Company App delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getCompanyApp(ServerProfile profile, String company)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, 
                                        "companies/" + company + "/apps");
        if(response == null) return new ArrayList();

        JSONArray companyApps = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"companyApps\": " + payload + "}";

            JSONParser parser = new JSONParser();                
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            companyApps    = (JSONArray)obj1.get("companyApps");

        } catch (ParseException pe){
            logger.error("Get Company App parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Company Apps error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return companyApps;
    }	
    
  //Method to remove the apiProducts from App payload for config update option so that it does not create credentials everytime
    //https://github.com/apigee/apigee-config-maven-plugin/issues/17
    public static String removeApiProductFromApp(ServerProfile profile, String appPayload){
		if(profile.getIgnoreProductsForApp()) {
			logger.info("Ignoring the API Product config from App");
			JsonParser parser = new JsonParser();
			JsonElement jsonElement = parser.parse(appPayload);
			JsonObject appJsonObj = jsonElement.getAsJsonObject();
			appJsonObj.remove("apiProducts");
			return appJsonObj.toString();
		}else
			return appPayload; 	
	}
}




