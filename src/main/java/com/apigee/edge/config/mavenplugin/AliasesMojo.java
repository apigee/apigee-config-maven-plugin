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
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.util.Key;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**                                                                                                                                     ¡¡
 * Goal to create aliases in a keystore in Apigee EDGE.
 * scope: env
 *
 * @author ahmed.fakhri
 * @goal aliases
 * @phase verify
 */

public class AliasesMojo extends GatewayAbstractMojo
{
    static Logger logger = LoggerFactory.getLogger(AliasesMojo.class);
    public static final String ____ATTENTION_MARKER____ =
            "************************************************************************";

    public static final String ALIAS_CERTIFICATE_ONLY_TYPE = "certificateonly";
    public static final String ALIAS_CERTIFICATE_AND_KEY_TYPE = "certificateandkey";

    public static final String ALIAS_CERTIFICATE_ONLY_FORMAT = "keycertfile";
    public static final String ALIAS_CERTIFICATE_AND_KEY_FORMAT = "keycertfile";

    enum OPTIONS {
        none, create, update, delete, sync
    }

    OPTIONS buildOption = OPTIONS.none;

    private ServerProfile serverProfile;

    public static class Alias {
        @Key
        public String name;
        
        public String store;
        public String type;
        public String ignoreExpiryValidation;
        public String ignoreNewlineValidation;
        public String certFilePath;
        public String keyFilePath;

    }

    public AliasesMojo() {
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

	/*
	 * protected String getAliasName(String payload) throws MojoFailureException {
	 * Gson gson = new Gson(); try { Alias alias = gson.fromJson(payload,
	 * Alias.class); return alias.name; } catch (JsonParseException e) { throw new
	 * MojoFailureException(e.getMessage()); } }
	 * 
	 * protected String getAliasType(String payload) throws MojoFailureException {
	 * Gson gson = new Gson(); try { Alias alias = gson.fromJson(payload,
	 * Alias.class); return alias.type; } catch (JsonParseException e) { throw new
	 * MojoFailureException(e.getMessage()); } }
	 */

    protected Alias getAlias(String payload) throws MojoFailureException {
        Gson gson = new Gson();
        try {
            Alias alias = gson.fromJson(payload, Alias.class);
            return alias;
        } catch (JsonParseException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    protected void doExecute(List<String> aliases) throws MojoFailureException {
        try {
            if (buildOption != OPTIONS.update &&
                    buildOption != OPTIONS.create &&
                    buildOption != OPTIONS.delete &&
                    buildOption != OPTIONS.sync) {
                return;
            }

            logger.info("Retrieving existing environment Aliases - " +
                    serverProfile.getEnvironment());

            Alias alias;
            List existingAliases = null;
            for (String aliasPayload : aliases) {
            	alias = getAlias(aliasPayload);
                String aliasName = alias.name;
                if (aliasName == null) {
                    throw new IllegalArgumentException(
                            "Alias does not have a name.\n" + alias + "\n");
                }
                
                existingAliases = getAliases(serverProfile, alias.store);
                if (existingAliases.contains(aliasName)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Alias \"" + aliasName + "\" exists. Updating.");
						    updateAlias(serverProfile, alias);
						    break;
                        case create:
                            logger.info("Alias \"" + aliasName + "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Alias \"" + aliasName + "\" already exists. Deleting.");
                            deleteAlias(serverProfile, alias);
                            break;
                        case sync:
						    logger.info("Alias \"" + aliasName + "\" already exists. Deleting and recreating."); 
						    deleteAlias(serverProfile, alias); 
						    logger.info("Creating Alias - " + aliasName);
						    createAlias(serverProfile, alias);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Alias - " + aliasName);
                            createAlias(serverProfile, alias);
                            break;
                        case delete:
                            logger.info("Alias \"" + aliasName +
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

        Logger logger = LoggerFactory.getLogger(AliasesMojo.class);

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
                logger.info(
                        "No Aliases config found.");
                return;
            }

            doExecute(aliases);

        } catch (MojoFailureException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    /***************************************************************************
     * REST call wrappers
     * @throws MojoFailureException 
     **/
    public static String createAlias(ServerProfile profile, Alias alias)
            throws IOException, MojoFailureException {
    	RestUtil restUtil = new RestUtil(profile);
    	    	
    	if (alias.store == null) {
    		throw new MojoFailureException("Apigee alias failure missinng 'store' for alias  '" + alias.name + "'");
		}

    	// Build files map and query parameters map
        Map<String, String> multipartFiles = new HashMap<String, String>();
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("alias", alias.name);
        parameters.put("ignoreExpiryValidation", alias.ignoreExpiryValidation);
        parameters.put("ignoreNewlineValidation", alias.ignoreExpiryValidation);

        if (ALIAS_CERTIFICATE_ONLY_TYPE.equalsIgnoreCase(alias.type)) {
        	if (alias.certFilePath == null) {
        		throw new MojoFailureException("Apigee alias failure : missing 'certFilePAth' for alias '" + alias.name + "'");
			}
        	multipartFiles.put("certFile", alias.certFilePath);
            parameters.put("format", ALIAS_CERTIFICATE_ONLY_FORMAT);

		}
        if (ALIAS_CERTIFICATE_AND_KEY_TYPE.equalsIgnoreCase(alias.type)) {
        	if (alias.certFilePath == null || alias.keyFilePath == null) {
        		throw new MojoFailureException("Apigee alias failure : missing 'certFilePAth' or 'keyFilePath' for alias '" + alias.name + "'");
			}
            parameters.put("format", ALIAS_CERTIFICATE_AND_KEY_FORMAT);
        	multipartFiles.put("certFile", alias.certFilePath);
        	multipartFiles.put("keyFile", alias.keyFilePath);
            parameters.put("privateKeyExportable", alias.ignoreExpiryValidation);
		}
        
		// Call rest helper
		HttpResponse response = restUtil.createEnvConfig(profile, 
        		"keystores", alias.store, "aliases", multipartFiles, parameters);
        
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

    public static String updateAlias(ServerProfile profile, Alias alias)
            throws IOException, MojoFailureException {
    	RestUtil restUtil = new RestUtil(profile);
    	    	
    	if (alias.store == null) {
    		throw new MojoFailureException("Apigee alias failure missinng 'store' for alias  '" + alias.name + "'");
		}

    	// INFO :  The Edge API support only update of cert in alias. There is no key update support.
    	
    	// Build files map and query parameters map
        Map<String, String> multipartFiles = new HashMap<String, String>();
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("alias", alias.name);
        parameters.put("ignoreExpiryValidation", alias.ignoreExpiryValidation);

        if (ALIAS_CERTIFICATE_ONLY_TYPE.equalsIgnoreCase(alias.type)) {
        	if (alias.certFilePath == null) {
        		throw new MojoFailureException("Apigee alias failure : missing 'certFilePAth' for alias '" + alias.name + "'");
			}
        	multipartFiles.put("file", alias.certFilePath);
		}
        
        if (ALIAS_CERTIFICATE_AND_KEY_TYPE.equalsIgnoreCase(alias.type)) {
        	if (alias.certFilePath == null) {
        		throw new MojoFailureException("Apigee alias failure : missing 'certFilePAth' for alias '" + alias.name + "'");
			}
        	multipartFiles.put("file", alias.certFilePath);
		}
        
		// Call rest helper
		HttpResponse response = restUtil.updateEnvConfig(profile, 
        		"keystores", alias.store, "aliases", alias.name, multipartFiles, parameters);
        
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("Alias update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteAlias(ServerProfile profile,
                                     Alias alias)
            throws IOException {
    	
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteEnvConfig(profile,
                "keystores",
                alias.store,
                "aliases",
                alias.name);
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

    public static List getAliases(ServerProfile profile, String keystoreName)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.listEnvConfig(profile, "keystores", keystoreName, "aliases");
        if(response == null) return new ArrayList();
        JSONArray aliases = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String tweekedPayload = "{ \"aliases\": " + payload + "}";

            JSONParser parser = new JSONParser();
            JSONObject jsonObject     = (JSONObject)parser.parse(tweekedPayload);
            aliases = (JSONArray)jsonObject.get("aliases");

        } catch (ParseException pe){
            logger.error("Get Keystore parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Keystore error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return aliases;
    }
    
}
