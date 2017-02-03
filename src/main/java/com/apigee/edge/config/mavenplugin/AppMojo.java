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

import java.net.URLEncoder;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.google.api.client.http.*;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**                                                                                                                                     ¡¡
 * Goal to create Apps in Apigee EDGE
 * scope: org
 *
 * @author madhan.sadasivam
 * @goal apps
 * @phase install
 */

public class AppMojo extends GatewayAbstractMojo
{
	static Logger logger = LoggerFactory.getLogger(AppMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class App {
        @Key
        public String name;
    }
	
    public static class KeyData {
        @Key
        public String consumerKey;
    }

	public AppMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee App");
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

	protected String getAppName(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			App app = gson.fromJson(payload, App.class);
			return app.name;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

    protected String getKeyName(String payload)
            throws MojoFailureException {
        Gson gson = new Gson();
        try {
            KeyData key = gson.fromJson(payload, KeyData.class);
            return key.consumerKey;
        } catch (JsonParseException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

	protected void doUpdate(Map<String, List<String>> devApps) 
            throws MojoFailureException {
		try {
			List existingApps = null;
			if (buildOption != OPTIONS.update && 
                buildOption != OPTIONS.create &&
				buildOption != OPTIONS.delete &&
                buildOption != OPTIONS.sync) {
				return;
			}

            for (Map.Entry<String, List<String>> entry : devApps.entrySet()) {

                String developerId = entry.getKey();
                logger.info("Retrieving Apps of " + developerId);
                existingApps = getApp(serverProfile, developerId);

    	        for (String app : entry.getValue()) {
    	        	String appName = getAppName(app);
    	        	if (appName == null) {
    	        		throw new IllegalArgumentException(
    	        			"App does not have a name.\n" + app + "\n");
    	        	}

            		if (existingApps.contains(appName)) {
                        switch (buildOption) {
                            case update:
                                logger.info("App \"" + appName + 
                                                        "\" exists. Updating.");
                                updateApp(serverProfile, developerId,
                                                        appName, app);
                                doUpdateKeys(serverProfile, developerId,
                                                        appName, app);
                                break;
                            case create:
                                logger.info("App \"" + appName + 
                                                "\" already exists. Skipping.");
                                doUpdateKeys(serverProfile, developerId,
                                                        appName, app);
                                break;
                            case delete:
                                logger.info("App \"" + appName + 
                                                "\" already exists. Deleting.");
                                deleteApp(serverProfile, developerId, appName);
                                break;
                            case sync:
                                logger.info("App \"" + appName + 
                                                "\" already exists. Deleting and recreating.");
                                deleteApp(serverProfile, developerId, appName);
                                logger.info("Creating App - " + appName);
                                createApp(serverProfile, developerId, app);
                                doUpdateKeys(serverProfile, developerId,
                                                        appName, app);
                                break;
                        }
    	        	} else {
                        switch (buildOption) {
                            case create:
                            case sync:
                            case update:
                                logger.info("Creating App - " + appName);
                                createApp(serverProfile, developerId, app);
                                break;
                            case delete:
                                logger.info("App \"" + appName + 
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

    private void doUpdateKeys(ServerProfile profile,
                              String developerId,
                              String appName,
                              String app)
            throws MojoFailureException {

        try {
            List existingKeys = null;
            logger.info("Retrieving Keys of " + appName);
            existingKeys = getKey(serverProfile, developerId, appName);

            List keys = getConfigSubArray(logger, app, "keys");

            if (keys == null || keys.size() == 0) {
                logger.info("No keys for App \"" + appName +
                        "\" found in edge.json.");
                return;
            }

            for (Object keyData : keys) {
                String key = ((JSONObject) keyData).toJSONString();
                String keyName = getKeyName(key);
                if (keyName == null) {
                    throw new IllegalArgumentException("Key for App \"" +
                            appName + "\" does not have a name.\n" + key + "\n");
                }

                if (existingKeys.contains(keyName)) {
                    switch (buildOption) {
                    case update:
                    case create:
                        logger.info("Key \"" + keyName + "\" for App \"" +
                                appName + "\" already exists. Skipping.");
                        doUpdateKeyProducts(serverProfile, developerId,
                                appName, keyName, key);
                        break;
                    case delete:
                        logger.info("Key \"" + keyName + "\" for App \"" +
                                appName + "\" already exists. Deleting.");
                        deleteKey(serverProfile, developerId, appName, key);
                        break;
                    case sync:
                        logger.info("Key \"" + keyName + "\" for App \"" + appName +
                                "\" already exists. Deleting and recreating.");
                        deleteKey(serverProfile, developerId, appName, key);
                        logger.info("Creating Key - " + keyName + " for App " +
                                appName);
                        createKey(serverProfile, developerId, appName, key);
                        doUpdateKeyProducts(serverProfile, developerId,
                                appName, keyName, key);
                        break;
                    }
                } else {
                    switch (buildOption) {
                    case create:
                    case sync:
                    case update:
                        logger.info("Creating Key - " + keyName + " for App " +
                                appName);
                        createKey(serverProfile, developerId, appName, key);
                        doUpdateKeyProducts(serverProfile, developerId,
                                appName, keyName, key);
                        break;
                    case delete:
                        logger.info("Key \"" + keyName + "\" for App \"" +
                                appName + "\" does not exist. Skipping.");
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

    private void doUpdateKeyProducts(ServerProfile profile,
                                     String developerId,
                                     String appName,
                                     String keyName,
                                     String key)
            throws MojoFailureException {

        try {
            List existingProducts = null;
            logger.info("Retrieving apiproducts of " + keyName + " of " + appName);
            existingProducts = getKeyProducts(serverProfile, developerId,
                    appName, keyName);

            List products = getConfigSubArray(logger, key, "apiProducts");

            if (products == null || products.size() == 0) {
                logger.info("No products for Key \"" + keyName +
                        "\" for App \"" + appName + "\" found in edge.json.");
                return;
            }

            switch (buildOption) {
            case update:
            case create:
                logger.info("Updating API Products for Key \"" + keyName +
                        "\" for App \"" + appName + ".\"");
                updateKeyProducts(serverProfile, developerId, appName, keyName,
                        key);
                break;
            case delete:
                logger.info("Key \"" + keyName + "\" for App \"" + appName +
                        "\" already exists. Deleting products.");
                deleteAllKeyProducts(serverProfile, developerId, appName,
                        keyName, existingProducts, products);
                break;
            case sync:
                logger.info("Key \"" + keyName + "\" for App \"" + appName +
                        "\" already exists. Deleting and Recreating products.");
                deleteAllKeyProducts(serverProfile, developerId, appName,
                        keyName, existingProducts, products);
                logger.info("Updating API Products for Key \"" + keyName +
                        "\" for App \"" + appName + ".\"");
                updateKeyProducts(serverProfile, developerId, appName, keyName,
                        key);
                break;
            }

        } catch (IOException e) {
            throw new MojoFailureException("Apigee network call error " +
                e.getMessage());
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private void deleteAllKeyProducts(ServerProfile serverProfile,
                                      String developerId,
                                      String appName,
                                      String keyName,
                                      List existingProducts,
                                      List products)
            throws IOException {

        for (Object product : products) {
            String apiProduct = (String) product;
            if (existingProducts.contains(product)) {
                logger.info("Product \" + product + \" for Key \"" + keyName +
                        "\" for App \"" + appName + "\" already exists. Deleting.");
                deleteKeyProducts(serverProfile, developerId, appName, keyName,
                        apiProduct);
            } else {
                logger.info("Product \" + product + \" for Key \"" + keyName +
                        "\" for App \"" + appName + "\" does not exist. Skipping.");
            }
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

		Logger logger = LoggerFactory.getLogger(AppMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Apps (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			Map<String, List<String>> apps = getOrgConfigWithId(logger, "developerApps");
			if (apps == null || apps.size() == 0) {
				logger.info("No developers apps found.");
                return;
			}

            logger.debug(apps.toString());
			doUpdate(apps);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

	public static List getConfigSubArray(Logger logger,
                                         String payload,
			                             String token)
               throws IOException {

		JSONArray arr1 = null;
		try {
			JSONParser parser = new JSONParser();
			JSONObject obj1 = (JSONObject) parser.parse(payload);
			arr1 = (JSONArray) obj1.get(token);

		} catch (ParseException pe) {
			logger.error("Get App parse error " + pe.getMessage());
			throw new IOException(pe.getMessage());
		}

		return arr1;
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createApp(ServerProfile profile, 
                                    String developerId,
                                    String app)
            throws IOException {

        HttpResponse response = RestUtil.createOrgConfig(profile, 
                                        "developers/" + developerId + "/apps",
                                         app);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("App create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateApp(ServerProfile profile,
                                    String developerId, 
                                    String appName, 
                                    String app)
            throws IOException {

        HttpResponse response = RestUtil.updateOrgConfig(profile, 
                                        "developers/" + developerId + "/apps", 
                                        appName,
                                        app);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("App update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteApp(ServerProfile profile,
                                    String developerId, 
                                    String appName)
            throws IOException {

        HttpResponse response = RestUtil.deleteOrgConfig(profile, 
                                        "developers/" + developerId + "/apps", 
                                        appName);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("App delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getApp(ServerProfile profile, String developerId)
            throws IOException {

        HttpResponse response = RestUtil.getOrgConfig(profile, 
                                        "developers/" + developerId + "/apps");

        if(response == null) return new ArrayList();

        JSONArray apps = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"apps\": " + payload + "}";

            JSONParser parser = new JSONParser();                
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            apps    = (JSONArray)obj1.get("apps");

        } catch (ParseException pe){
            logger.error("Get App parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Apps error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return apps;
    }

    public static String createKey(ServerProfile profile,
                                   String developerId,
                                   String appName,
                                   String key)
            throws IOException {

        HttpResponse response = RestUtil.createOrgConfig(profile,
                "developers/" + developerId + "/apps/" + URLEncoder.encode(appName, "UTF-8") + "/keys/create",
                key);

        try {

            logger.info("Response " + response.getContentType() + "\n" +
                             response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Key create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteKey(ServerProfile profile,
                                   String developerId,
                                   String appName,
                                   String key)
            throws IOException {

        HttpResponse response = RestUtil.deleteOrgConfig(profile,
                "developers/" + developerId + "/apps/" + URLEncoder.encode(appName, "UTF-8") + "/keys",
                key);

        try {

            logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Key delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getKey(ServerProfile profile,
                              String developerId,
                              String appName)
            throws IOException {

        HttpResponse response = RestUtil.getOrgConfig(profile,
                "developers/" + developerId + "/apps/" + URLEncoder.encode(appName, "UTF-8"));

        if(response == null) return new ArrayList();

        List keys = new ArrayList();
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            JSONParser parser = new JSONParser();
            JSONObject obj1 = (JSONObject) parser.parse(payload);
            JSONArray arr1 = (JSONArray) obj1.get("credentials");

            if(arr1 == null) return new ArrayList();

            // flatten array
            for (Object obj2 : arr1) {
                JSONObject obj3 = (JSONObject) obj2;
                keys.add(obj3.get("consumerKey"));
            }

        } catch (ParseException pe) {
            logger.error("Get App parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Apps error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return keys;
    }

    public static String updateKeyProducts(ServerProfile profile,
                                           String developerId,
                                           String appName,
                                           String keyName,
                                           String products)
            throws IOException {

        HttpResponse response = RestUtil.updateOrgConfig(profile,
                "developers/" + developerId + "/apps/" + URLEncoder.encode(appName, "UTF-8") +
                "/keys", keyName, products);

        try {

            logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("ApiProduct update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteKeyProducts(ServerProfile profile,
                                           String developerId,
                                           String appName,
                                           String keyName,
                                           String productName)
            throws IOException {

        HttpResponse response = RestUtil.deleteOrgConfig(profile,
                "developers/" + developerId + "/apps/" + URLEncoder.encode(appName, "UTF-8") +
                "/keys/" + keyName + "/apiproducts", productName);

        try {

            logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("App delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getKeyProducts(ServerProfile profile,
                                      String developerId,
                                      String appName,
                                      String keyName)
            throws IOException {

        HttpResponse response = RestUtil.getOrgConfig(profile,
                "developers/" + developerId + "/apps/" + URLEncoder.encode(appName, "UTF-8") +
                "/keys/" + keyName);

        if(response == null) return new ArrayList();

        List products = new ArrayList();
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            JSONParser parser = new JSONParser();
            JSONObject obj1 = (JSONObject) parser.parse(payload);
            JSONArray arr1 = (JSONArray) obj1.get("apiproducts");

            if(arr1 == null) return new ArrayList();

            // flatten array
            for (Object obj2 : arr1) {
                JSONObject obj3 = (JSONObject) obj2;
                products.add(obj3.get("apiproduct"));
            }

        } catch (ParseException pe) {
            logger.error("Get App parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Apps error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return products;
    }
}
