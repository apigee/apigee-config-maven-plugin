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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**                                                                                                                                     ¡¡
 * Goal to export App Group Dev app keys to a file
 * scope: org
 *
 * @author ssvaidyanathan
 * @goal exportAppGroupAppKeys
 * @phase install
 */

public class ExportAppGroupAppKeysMojo extends GatewayAbstractMojo
{
	static Logger logger = LogManager.getLogger(ExportAppGroupAppKeysMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	private ServerProfile serverProfile;
	
	private String exportDir;

    public static class AppGroupApp {
        @Key
        public String name;
        @Key
        public List<Credentials> credentials;
    }
	
    public static class Credentials {
    	@Key
    	public String consumerKey;
    	@Key
    	public String consumerSecret;
    	@Key
    	public List<ApiProducts> apiProducts;
    }
    
    public static class ApiProducts {
    	@Key
    	public String apiproduct;
    }
    
	public ExportAppGroupAppKeysMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Export App Group App Keys");
			logger.info(____ATTENTION_MARKER____);

			serverProfile = super.getProfile();			
			exportDir = super.getExportDir();
			logger.debug("exportDir " + exportDir);
			logger.debug("Base dir " + super.getBaseDirectoryPath());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Invalid apigee.option provided");
		} catch (RuntimeException e) {
			throw e;
		}

	}

	protected String getAppGroupAppName(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			AppGroupApp app = gson.fromJson(payload, AppGroupApp.class);
			return app.name;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doExport(Map<String, List<String>> apps) 
            throws MojoFailureException {
		if(exportDir==null || exportDir ==""){
    		throw new MojoFailureException("Please provide the directory where the appGroupAppKeys.json file should be exported (-Dapigee.config.exportDir)");
    	}
		try {
			List<String> existingApps = null;
			List<AppGroupApp> appGroupApps = new ArrayList<AppGroupApp>();
            for (Map.Entry<String, List<String>> entry : apps.entrySet()) {

                String appGroupId = URLEncoder.encode(entry.getKey(), "UTF-8");
                logger.info("Retrieving App Group Apps of " + appGroupId);
                existingApps = getAppGroupApp(serverProfile, appGroupId);
                
                if(existingApps!=null && existingApps.size()>0){
                	for (String existingApp : existingApps) {
						logger.info("Fetching App Group App info for: "+existingApp);
						appGroupApps.add(getAppGroupAppDetails(serverProfile, appGroupId, existingApp));
					}
                }
            }
            exportToFile(serverProfile, appGroupApps, exportDir);
		} catch (IOException e) {
			throw new MojoFailureException("Apigee network call error " +
														 e.getMessage());
		} catch (RuntimeException e) {
			throw e;
		}
	}

	public static void exportToFile(ServerProfile profile, List<AppGroupApp> apps, String exportFilePath)
            throws IOException, MojoFailureException {
		BufferedWriter bw = null;
		FileWriter fw = null;

		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String payload = gson.toJson(apps);
			logger.debug("export payload: "+payload);
			fw = new FileWriter(exportFilePath+ File.separator+"appGroupAppKeys.json");
			bw = new BufferedWriter(fw);
			bw.write(payload);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
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

		Logger logger = LogManager.getLogger(ExportAppGroupAppKeysMojo.class);

		try {
			
			init();

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			Map<String, List<String>> apps = getOrgConfigWithId(logger, "appGroupApps");
			if (apps == null || apps.size() == 0) {
				logger.info("No App Group apps found.");
                return;
			}

            logger.debug("AppGroupApps:" +apps.toString());
            doExport(apps);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
	
	public static List getAppGroupApp(ServerProfile profile, String appGroupId)
            throws IOException, MojoFailureException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, 
                                        "appgroups/" + appGroupId + "/apps");
        if(response == null) return new ArrayList<String>();
       JSONArray appsList = new JSONArray();
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            JSONParser parser = new JSONParser();       
            JSONObject obj     = (JSONObject)parser.parse(payload);
            JSONArray appsArray    = (JSONArray)obj.get("appGroupApps");
            for (int i = 0; appsArray != null && i < appsArray.size(); i++) {
             	 JSONObject a = (JSONObject) appsArray.get(i);
             	appsList.add(a.get("name"));
           }
        } catch (ParseException pe){
            logger.error("Get App parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Apps error " + e.getMessage());
            throw new IOException(e.getMessage());
        }
        return appsList;
    }	
    
    public static AppGroupApp getAppGroupAppDetails(ServerProfile profile, String appGroupId, String app)
            throws IOException, MojoFailureException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, 
                                        "appgroups/" + appGroupId + "/apps/"+app);
        AppGroupApp appObj = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            
            Gson gson = new Gson();
    		try {
    			appObj = gson.fromJson(payload, AppGroupApp.class);
    		} catch (JsonParseException e) {
    			e.printStackTrace();
    		  throw new MojoFailureException(e.getMessage());
    		}
        } catch (HttpResponseException e) {
            logger.error("Get Apps error " + e.getMessage());
            throw new IOException(e.getMessage());
        }
        return appObj;
    }	
}




