/**
 * Copyright 2023 Google LLC
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**                                                                                                                                     ¡¡
 * Goal to create API Docs in Apigee Portal
 * scope: org
 *
 * @author ssvaidyanathan
 * @goal apidocs
 * @phase install
 */

public class APIDocsMojo extends GatewayAbstractMojo
{
	static Logger logger = LogManager.getLogger(APIDocsMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;
	
	public APIDocsMojo() {
		super();

	}

	public static class APIDoc {
        @Key
        public String title;
        @Key
        public List<String> categories;
    }
	
	protected String getAPIDocName(String payload) throws MojoFailureException {
		Gson gson = new Gson();
		try {
			APIDoc apiDoc = gson.fromJson(payload, APIDoc.class);
			return apiDoc.title;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}
	
	protected String updatePayloadWithCategoryId(String payload, ServerProfile profile) throws MojoFailureException, IOException {
		Gson gson = new Gson();
		try {
			APIDoc apiDoc = gson.fromJson(payload, APIDoc.class);
			List<String> categories = apiDoc.categories;
			if(categories!=null && categories.size()>0) {
				//Fetch existing categories and its id from portal
				Map<String, String> existingCategoryMap = getCategories(profile);
				if(existingCategoryMap != null && existingCategoryMap.size()>0) {
					JsonArray categoryIds = new JsonArray();
					for (String category : categories) {
						JsonPrimitive id = new JsonPrimitive(existingCategoryMap.get(category));
						categoryIds.add(id);
					}
					JsonParser parser = new JsonParser();
					JsonElement jsonElement = parser.parse(payload);
					JsonObject apiDocJsonObj = jsonElement.getAsJsonObject();
					apiDocJsonObj.add("categoryIds", categoryIds);
					apiDocJsonObj.remove("categories"); //remove the categories
					return apiDocJsonObj.toString();
				}
			}
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
		return payload;
	}
	

	public void init() throws MojoExecutionException, MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Portal API Docs");
			logger.info(____ATTENTION_MARKER____);

			String options="";
			serverProfile = super.getProfile();			
	
			options = super.getOptions();
			if (options != null) {
				buildOption = OPTIONS.valueOf(options);
			}
			logger.debug("Build option " + buildOption.name());
			logger.debug("Base dir " + super.getBaseDirectoryPath());
			if (serverProfile.getPortalSiteId() == null) {
		        throw new MojoExecutionException(
		          "Portal Site ID not found in profile");
		      }
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Invalid apigee.option provided");
		} catch (RuntimeException e) {
			throw e;
		}

	}

	protected void doUpdate(List<String> apiDocs) 
            throws MojoFailureException {
		try {
			Map<String, String> existingDocs = null;
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create &&
                buildOption != OPTIONS.delete &&
                buildOption != OPTIONS.sync) {
				return;
			}
			logger.info("Retrieving existing API Docs - " +
                    serverProfile.getEnvironment());
			existingDocs = getAPIDocs(serverProfile);

	        for (String apiDoc : apiDocs) {
	        	//update category with categoryId
	        	apiDoc = updatePayloadWithCategoryId(apiDoc, serverProfile);
	        	logger.debug("updated doc: "+ apiDoc);
	        	String apiDocName = getAPIDocName(apiDoc);
	        	 if (apiDocName == null) {
	        		throw new IllegalArgumentException(
	        		   "API Doc does not have a title.\n" + apiDoc + "\n");
	        	}
	        	if (existingDocs != null && existingDocs.keySet()!=null 
	        			&& existingDocs.keySet().contains(apiDocName)) {
                    switch (buildOption) {
                        case update:
                        	logger.info("Updating API Doc - " + apiDocName);
                        	updateAPIDoc(serverProfile, existingDocs.get(apiDocName), apiDoc);
                        	createAPIDocSpec(serverProfile, existingDocs.get(apiDocName), apiDoc);
                        	break;
                        case create:
        			        logger.info("API Doc \"" + apiDocName + 
    									"\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("API Doc \"" + apiDocName + 
                                        "\" already exists. Deleting.");
                            deleteAPIDoc(serverProfile, existingDocs.get(apiDocName));
                            break;
                        case sync:
                            logger.info("API Doc \"" + apiDocName + 
                                        "\" already exists. Deleting and recreating.");
                            deleteAPIDoc(serverProfile, existingDocs.get(apiDocName));
                            logger.info("Creating API Doc - " + apiDoc);
                            String apiDocId = createAPIDoc(serverProfile, apiDoc);
                            createAPIDocSpec(serverProfile, apiDocId, apiDoc);
                            break;
	        		}
	        	} else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating API Doc - " + apiDocName);
                            String apiDocId = createAPIDoc(serverProfile, apiDoc);
                            createAPIDocSpec(serverProfile, apiDocId, apiDoc);
                            break;
                        case delete:
                            logger.info("API Doc \"" + apiDocName + 
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
			logger.info("Skipping");
			return;
		}

		Logger logger = LogManager.getLogger(APIDocsMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping API Docs (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			List apiDocs = getOrgConfig(logger, "apiDocs");
			if (apiDocs == null || apiDocs.size() == 0) {
				logger.info("No API Docs found.");
                return;
			}
			
			doUpdate(apiDocs);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createAPIDoc(ServerProfile profile, String apiDocPayload)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        try {
        	JsonElement jsonObj= new Gson().fromJson(apiDocPayload, JsonElement.class);
        	jsonObj.getAsJsonObject().remove("oasDocumentation");
        	jsonObj.getAsJsonObject().remove("graphqlDocumentation");
        	HttpResponse response = restUtil.createOrgConfig(profile, 
					"sites/"+profile.getPortalSiteId()+"/apidocs",
					jsonObj.toString());
        	String responseString = response.parseAsString();
        	logger.info("Response " + response.getContentType() + "\n" + responseString);
            if (response.isSuccessStatusCode()) {
            	logger.info("Create Success.");
                JsonElement respObj= new Gson().fromJson(responseString, JsonElement.class);
                String docId = respObj.getAsJsonObject().get("data").getAsJsonObject().get("id").getAsString();
                return docId;
            }
        } catch (HttpResponseException e) {
            logger.error("API Doc create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }
        return "";
    }
    
    public static String createAPIDocSpec(ServerProfile profile, String apiDocId, String apiDocPayload)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        try {
        	logger.info("API Documentation");
        	String specPayload = updatePayloadWithSpecContents(apiDocPayload);
        	HttpResponse response = restUtil.patchOrgConfig(profile, 
					"sites/"+profile.getPortalSiteId()+"/apidocs/"+apiDocId+"/documentation",
					specPayload);
        	
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("API Doc create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }
    
    public static String updateAPIDoc(ServerProfile profile, 
							            String apiDocId, 
							            String apiDocPayload)
	throws IOException {
	RestUtil restUtil = new RestUtil(profile);
	try {
		JsonElement jsonObj= new Gson().fromJson(apiDocPayload, JsonElement.class);
    	jsonObj.getAsJsonObject().remove("oasDocumentation");
    	jsonObj.getAsJsonObject().remove("graphqlDocumentation");
    	HttpResponse response = restUtil.updateOrgConfig(profile, 
                "sites/"+profile.getPortalSiteId()+"/apidocs", 
                apiDocId,
                jsonObj.toString());
		logger.info("Response " + response.getContentType() + "\n" +
		            response.parseAsString());
		if (response.isSuccessStatusCode())
		logger.info("Update Success.");

	} catch (HttpResponseException e) {
		logger.error("Target Server update error " + e.getMessage());
		throw new IOException(e.getMessage());
	}
	return "";
	}

    public static String deleteAPIDoc(ServerProfile profile,
                                            String apiDocId)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteOrgConfig(profile, 
                                                        "sites/"+profile.getPortalSiteId()+"/apidocs", 
                                                        apiDocId);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("API Doc delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }
    
    public static Map<String, String> getAPIDocs(ServerProfile profile)
            throws IOException {
    	Map<String, String> apiDocMap = new HashMap<String, String>();
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, "sites/"+profile.getPortalSiteId()+"/apidocs?pageSize=100");
        if(response == null) return apiDocMap;
        JSONArray apiDocs = null;
        try {
            logger.debug("output " + response.getContentType());
            String payload = response.parseAsString();
            logger.debug(payload);

            JSONParser parser = new JSONParser();                
            JSONObject obj    = (JSONObject)parser.parse(payload);
            apiDocs    = (JSONArray)obj.get("data");
            for (int i = 0; apiDocs != null && i < apiDocs.size(); i++) {
           	 JSONObject a = (JSONObject) apiDocs.get(i);
           	 apiDocMap.put((String)a.get("title"), (String)a.get("id"));
           }
        } catch (ParseException pe){
            logger.error("Get API Doc parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get API Doc error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return apiDocMap;
    }	
    
    public static Map<String, String> getCategories(ServerProfile profile)
            throws IOException {
    	Map<String, String> categoryMap = new HashMap<String, String>();
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, "sites/"+profile.getPortalSiteId()+"/apicategories");
        if(response == null) return categoryMap;
        JSONArray categories = null;
        try {
            logger.debug("output " + response.getContentType());
            String payload = response.parseAsString();
            logger.debug(payload);

            JSONParser parser = new JSONParser();                
            JSONObject obj    = (JSONObject)parser.parse(payload);
            categories    = (JSONArray)obj.get("data");
            for (int i = 0; categories != null && i < categories.size(); i++) {
           	 JSONObject a = (JSONObject) categories.get(i);
           	 categoryMap.put((String)a.get("name"), (String)a.get("id"));
           }
        } catch (ParseException pe){
            logger.error("Get Categories parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Categories error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return categoryMap;
    }
    
    public static String updatePayloadWithSpecContents(String payload) throws IOException {
		Gson gson = new Gson();
		try {
			JsonElement jsonObj= gson.fromJson(payload, JsonElement.class);
			JsonElement oas = jsonObj.getAsJsonObject().get("oasDocumentation");
			if(oas!=null) {
				JsonObject spec = oas.getAsJsonObject().get("spec").getAsJsonObject();
				spec.addProperty("contents", fileToBase64String(spec.get("file").getAsString()));
				spec.remove("file");
				return "{\"oasDocumentation\":"+oas.toString()+"}";
			}
			JsonElement gql = jsonObj.getAsJsonObject().get("graphqlDocumentation");
			if(gql!=null) {
				
				JsonObject schema = gql.getAsJsonObject().get("schema").getAsJsonObject();
				schema.addProperty("contents", fileToBase64String(schema.get("file").getAsString()));
				schema.remove("file");
				return "{\"graphqlDocumentation\":"+gql.toString()+"}";
			}
			return null;
		} catch (JsonParseException e) {
		  throw new IOException(e.getMessage());
		}
	}
	
    
    public static String fileToBase64String (String filePath) throws IOException {
		byte[] byteData;
		String base64String=null;
		try {
			byteData = Files.readAllBytes(Paths.get(filePath));
			base64String = Base64.getEncoder().encodeToString(byteData);
		} catch (IOException e) {
			throw new IOException(e.getMessage());
		}
        return base64String;
	}

}




