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
import java.net.URLEncoder;
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
 * Goal to create alias in Apigee EDGE
 * scope: env
 *
 * @author saisaran.vaidyanathan
 * @goal aliases
 * @phase install
 */

public class AliasMojo extends GatewayAbstractMojo
{
	static Logger logger = LoggerFactory.getLogger(AliasMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class Alias {
        @Key
        public String alias;
        @Key
        public String keystorename;
        @Key
        public String format;
        @Key
        public String certFilePath;
        @Key
        public String keyFilePath;
        @Key
        public String filePath;
        @Key
        public String password;
        @Key
        public boolean ignoreExpiryValidation;
        @Key
        public boolean ignoreNewlineValidation;
        @Key
        public boolean privateKeyExportable;
    }
	
	public AliasMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Alias");
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

	protected static Alias getAliasObj(String payload) throws MojoFailureException {
		Gson gson = new Gson();
		try {
			Alias alias = gson.fromJson(payload, Alias.class);
			return alias;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	/**
	 * create alias values
	 */
	protected void doUpdate(List<String> aliases) throws MojoFailureException {
		try {
			List existingAliases = null;
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create && 
                buildOption != OPTIONS.delete && 
                buildOption != OPTIONS.sync) {
				return;
			}

	        for (String alias : aliases) {
	        	
	        	Alias a = getAliasObj(alias);
	        	if (a.alias == null) {
	        		throw new IllegalArgumentException(
	        			"Alias does not have a name.\n" + alias + "\n");
	        	}
	        	if (a.keystorename == null) {
	        		throw new IllegalArgumentException(
	        			"Alias does not have a keystorename.\n" + alias + "\n");
	        	}
	        	
	        	logger.info("Retrieving existing environment aliases for keystore "+a.keystorename+" in "+ serverProfile.getEnvironment());
	        	existingAliases = getAlias(serverProfile, a.keystorename);	
        		if (existingAliases.contains(a.alias)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Alias \"" + a.alias + 
                                                    "\" exists.");
                            logger.info("Please delete the alias and re-create or run it using 'sync' option");
                            break;
                        case create:
                            logger.info("Alias \"" + a.alias + 
                                                "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Alias \"" + a.alias + 
                                                "\" already exists. Deleting.");
                            deleteAlias(serverProfile, alias);
                            break;
                        case sync:
                            logger.info("Alias \"" + a.alias + 
                                                "\" already exists. Deleting and recreating.");
                            deleteAlias(serverProfile, alias);
                            logger.info("Creating Alias - " + a.alias);
                            createAlias(serverProfile, alias);
                            break;
                    }
	        	} else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Alias - " + a.alias);
                            createAlias(serverProfile, alias);
                            break;
                        case delete:
                            logger.info("Alias \"" + a.alias + 
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

		Logger logger = LoggerFactory.getLogger(AliasMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Alias (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			List aliases = getEnvConfig(logger, "aliases");
			if (aliases == null || aliases.size() == 0) {
				logger.info("No alias config found.");
                return;
			}

			doUpdate(aliases);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createAlias(ServerProfile profile, String aliasPayload)
            throws IOException, MojoFailureException {
    	RestUtil restUtil = new RestUtil(profile);
    	Alias alias = getAliasObj(aliasPayload);
    	
    	validateAlias(alias);
    	
    	// Call rest helper
    	HttpResponse response = null;
    	//For selfsignedcert, pass the JSON payload
    	if(alias.format!=null && alias.format.equalsIgnoreCase("selfsignedcert")) {
    		Map<String, String> params = new HashMap<String, String>();
    		params.put("format", "selfsignedcert");
    		response = restUtil.createEnvConfigWithParameters(profile, 
    				"keystores", URLEncoder.encode(alias.keystorename, "UTF-8"), "aliases", params, aliasPayload);
    	}
    	else {
    		
    		Map<String, String> parameters = new HashMap<String, String>();
        	parameters.put("alias", alias.alias);
        	parameters.put("format", alias.format);
        	parameters.put("ignoreNewlineValidation", (alias.ignoreNewlineValidation)?String.valueOf(alias.ignoreNewlineValidation): "true"); //default to true
        	parameters.put("ignoreExpiryValidation", (alias.ignoreExpiryValidation)?String.valueOf(alias.ignoreExpiryValidation): "false"); //default to false
        	parameters.put("privateKeyExportable", (alias.privateKeyExportable)?String.valueOf(alias.privateKeyExportable): "false"); //default to false
        	if(alias.password!=null && !alias.password.equalsIgnoreCase(""))
        		parameters.put("password", alias.password);   
        	
    		Map<String, String> multipartFiles = new HashMap<String, String>();
    		//Set param as "certFile" for keycertfile
    		if(alias.certFilePath!=null && !alias.certFilePath.equalsIgnoreCase(""))
    			multipartFiles.put("certFile", alias.certFilePath);
    		//Set param as "keyFile" for keycertfile if keyFilePath is passed
        	if(alias.keyFilePath!=null && !alias.keyFilePath.equalsIgnoreCase(""))
        		multipartFiles.put("keyFile", alias.keyFilePath);  
        	//Set param as "file" for keycertjar or pkcs12
        	if(alias.filePath!=null && !alias.filePath.equalsIgnoreCase(""))
    			multipartFiles.put("file", alias.filePath);

    		response = restUtil.createEnvConfigUpload(profile, 
            		"keystores", alias.keystorename, "aliases", multipartFiles, parameters);
    	}
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Alias create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteAlias(ServerProfile profile, 
                                        String aliasPayload)
            throws IOException, MojoFailureException {
    	Alias alias = getAliasObj(aliasPayload);
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteEnvConfig(profile, "keystores/"+URLEncoder.encode(alias.keystorename, "UTF-8")+"/aliases", alias.alias);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Alias delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getAlias(ServerProfile profile, String keystore)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getEnvConfig(profile, "keystores/"+URLEncoder.encode(keystore, "UTF-8")+"/aliases");
        if(response == null) return new ArrayList();
        JSONArray aliases = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"aliases\": " + payload + "}";

            JSONParser parser = new JSONParser();                
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            aliases    = (JSONArray)obj1.get("aliases");

        } catch (ParseException pe){
            logger.error("Get Alias parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Alias error " + e.getMessage());
            throw new IOException(e.getMessage());
        }
        return aliases;
    }	
    
    private static boolean validateAlias(Alias alias) throws MojoFailureException{
    	if (alias.alias == null || alias.alias.equalsIgnoreCase("")) {
    		throw new MojoFailureException("Apigee alias is missing 'alias' property");
		}
    	if (alias.keystorename == null || alias.format == null || alias.keystorename.equalsIgnoreCase("")|| alias.format.equalsIgnoreCase("")) {
    		throw new MojoFailureException("Apigee alias is missing 'keystorename' or 'format' for alias  '" + alias.alias + "' property");
		}
    	switch (alias.format) {
    		case "keycertfile":
    			if (alias.certFilePath == null || alias.certFilePath.equalsIgnoreCase("")) {
    	    		throw new MojoFailureException("Apigee alias is missing 'certFilePath' for alias  '" + alias.alias + "' property");
    			}
    			break;
    		
    		case "keycertjar":
    		case "pkcs12":
    			if (alias.filePath == null || alias.filePath.equalsIgnoreCase("")) {
    	    		throw new MojoFailureException("Apigee alias is missing 'filePath' for alias  '" + alias.alias + "' property");
    			}
    			break;
    	}
    	
    	return true;
    }
}




