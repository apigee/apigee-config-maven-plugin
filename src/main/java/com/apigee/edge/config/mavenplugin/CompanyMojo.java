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
 * Goal to create Company in Apigee EDGE
 * scope: org
 *
 * @author ssvaidyanathan
 * @goal companies
 * @phase install
 */

public class CompanyMojo extends GatewayAbstractMojo
{
	static Logger logger = LoggerFactory.getLogger(CompanyMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class Company {
        @Key
        public String name;
    }
	
	public CompanyMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Company");
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

	protected String getCompanyName(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			Company company = gson.fromJson(payload, Company.class);
			return company.name;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(List<String> companies) 
            throws MojoFailureException {
		try {
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create && 
                buildOption != OPTIONS.delete && 
                buildOption != OPTIONS.sync) {
				return;
			}

	        for (String company : companies) {
	        	String companyName = getCompanyName(company);
	        	if (companyName == null) {
	        		throw new IllegalArgumentException(
	        			"Company does not have a name.\n" + company + "\n");
	        	}

        		if (doesCompanyExist(serverProfile, companyName)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Company \"" + companyName + 
                                                    "\" exists. Updating.");
                            updateCompany(serverProfile,
                            		companyName, company);
                            break;
                        case create:
                            logger.info("Company \"" + companyName + 
                                                "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Company \"" + companyName + 
                                    "\" already exists. Deleting.");
                            deleteCompany(serverProfile, companyName);
                            break;
                        case sync:
                            logger.info("Company \"" + companyName + 
                                    "\" already exists. Deleting and recreating.");
                            deleteCompany(serverProfile, companyName);
                            logger.info("Creating Company - " + companyName);
                            createCompany(serverProfile, company);
                            break;
                    }
	        	} else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Company - " + companyName);
                            createCompany(serverProfile, company);
                            break;
                        case delete:
                            logger.info("Company \"" + companyName + 
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

		Logger logger = LoggerFactory.getLogger(CompanyMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Companies (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			List companies = getOrgConfig(logger, "companies");
			if (companies == null || companies.size() == 0) {
				logger.info("No Companies found.");
                return;
			}

			doUpdate(companies);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createCompany(ServerProfile profile, String company)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createOrgConfig(profile, 
                                                         "companies",
                                                         company);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Company create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateCompany(ServerProfile profile, 
                                        String companyName, 
                                        String company)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.updateOrgConfig(profile, 
                                                        "companies", 
                                                        companyName,
                                                        company);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("Company update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteCompany(ServerProfile profile, 
                                        String companyName)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteOrgConfig(profile, 
                                                        "companies", 
                                                        companyName);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Company delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getCompany(ServerProfile profile)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, "companies");
        if(response == null) return new ArrayList();
        JSONArray companies = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"companies\": " + payload + "}";

            JSONParser parser = new JSONParser();                
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            companies    = (JSONArray)obj1.get("companies");

        } catch (ParseException pe){
            logger.error("Get Company parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Company error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return companies;
    }	
    
    public static boolean doesCompanyExist(ServerProfile profile, String companyName)
            throws IOException {
        try {
        	logger.info("Checking if company - " +companyName + " exist");
        	RestUtil restUtil = new RestUtil(profile);
            HttpResponse response = restUtil.getOrgConfig(profile, "companies/"+URLEncoder.encode(companyName, "UTF-8"));
            if(response == null) 
            	return false;
        } catch (HttpResponseException e) {
            throw new IOException(e.getMessage());
        }

        return true;
    }	
}




