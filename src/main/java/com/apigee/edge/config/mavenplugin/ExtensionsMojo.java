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
import java.util.List;

/**                                                                                                                                     ¡¡
 * Goal to create Extensions in Apigee EDGE.
 * scope: env
 *
 * @author ssvaidyanathan
 * @goal extensions
 * @phase install
 */

public class ExtensionsMojo extends GatewayAbstractMojo
{
    static Logger logger = LoggerFactory.getLogger(ExtensionsMojo.class);
    public static final String ____ATTENTION_MARKER____ =
            "************************************************************************";

    enum OPTIONS {
        none, create, update, delete, sync
    }

    OPTIONS buildOption = OPTIONS.none;

    private ServerProfile serverProfile;

    public static class Extension {
        @Key
        public String name;
    }

    public ExtensionsMojo() {
        super();

    }

    public void init() throws MojoFailureException {
        try {
            logger.info(____ATTENTION_MARKER____);
            logger.info("Apigee Extensions");
            logger.info(____ATTENTION_MARKER____);

            String options="";
            serverProfile = super.getProfile();

            options = super.getOptions();
            if (options != null) {
                buildOption = OPTIONS.valueOf(options);
            }
            logger.debug("Build option " + buildOption.name());
            logger.debug("Base dir " + super.getBaseDirectoryPath());
            //Extensions Management API does not support Basic Auth (only OAuth)
            if(serverProfile.getAuthType()!=null && !serverProfile.getAuthType().equals("oauth")) {
            	logger.error("Extensions are not supported with basic auth. Please use oauth");
    			throw new MojoFailureException ("Extensions are not supported with basic auth. Please use oauth");
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid apigee.option provided");
        } catch (RuntimeException e) {
            throw e;
        }

    }

    protected String getExtensionName(String payload) throws MojoFailureException {
        Gson gson = new Gson();
        try {
            Extension extension = gson.fromJson(payload, Extension.class);
            return extension.name;
        } catch (JsonParseException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    protected void doUpdate(List<String> extensions) throws MojoFailureException {
        try {
            List existingExtensions = null;
            if (buildOption != OPTIONS.update &&
                    buildOption != OPTIONS.create &&
                    buildOption != OPTIONS.delete &&
                    buildOption != OPTIONS.sync) {
                return;
            }

            logger.info("Retrieving existing environment Extensions - " +
                    serverProfile.getEnvironment());
            existingExtensions = getExtension(serverProfile);

            for (String extension : extensions) {
                String extensionName = getExtensionName(extension);
                if (extensionName == null) {
                    throw new IllegalArgumentException(
                            "Extension does not have a name.\n" + extension + "\n");
                }

                if (existingExtensions.contains(extensionName)) {
                	logger.info("Retrieving ID for \"" + extensionName+"\"" );
                    String extensionId = getExtensionID(serverProfile, extensionName);
                    switch (buildOption) {
                        case update:
                            logger.info("Extension \"" + extensionName +
                                    "\" exists. Updating.");
                            updateExtension(serverProfile, extensionId, extension);
                            break;
                        case create:
                            logger.info("Extension \"" + extensionName +
                                    "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Extension \"" + extensionName +
                                    "\" already exists. Deleting.");
                            deleteExtension(serverProfile, extensionId);
                            break;
                        case sync:
                            logger.info("Extension \"" + extensionName +
                                    "\" already exists. Deleting and recreating.");
                            deleteExtension(serverProfile, extensionId);
                            logger.info("Creating Extension - " + extensionName);
                            createExtension(serverProfile, extension);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Extension - " + extensionName);
                            createExtension(serverProfile, extension);
                            break;
                        case delete:
                            logger.info("Extension \"" + extensionName +
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

        Logger logger = LoggerFactory.getLogger(ExtensionsMojo.class);

        try {

            init();

            if (buildOption == OPTIONS.none) {
                logger.info("Skipping Extension (default action)");
                return;
            }

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                        "Apigee environment not found in profile");
            }

            List extensions = getEnvConfig(logger, "extensions");
            if (extensions == null || extensions.size() == 0) {
                logger.info(
                        "No Extension config found.");
                return;
            }

            doUpdate(extensions);

        } catch (MojoFailureException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createExtension(ServerProfile profile, String extension)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createEnvConfig(profile,
                "extensions",
                extension);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Extension create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateExtension(ServerProfile profile,
                                     String extensionId,
                                     String extension)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.patchEnvConfig(profile,
                "extensions",
                extensionId,
                extension);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("Extension update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteExtension(ServerProfile profile,
                                     String extensionId)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteEnvConfig(profile,
                "extensions",
                extensionId);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Extension delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List<String> getExtension(ServerProfile profile)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getEnvConfig(profile, "extensions");
        if(response == null) return new ArrayList();
        List<String> extensionNames = new ArrayList<String>();
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);
            JSONParser parser = new JSONParser();
            JSONObject obj     = (JSONObject)parser.parse(payload);
            JSONArray extensions    = (JSONArray)obj.get("contents");
            if(extensions!=null && extensions.size()>0) {
            	for (Object extension : extensions) {
            		extensionNames.add(((JSONObject) extension).get("name").toString());
    			}
            }
        } catch (ParseException pe){
            logger.error("Get Extension parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Extension error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return extensionNames;
    }
    
    public static String getExtensionID(ServerProfile profile, String extensionName)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getEnvConfig(profile, "extensions?name="+extensionName);
        if(response == null) return null;
        String extensionId = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);
            JSONParser parser = new JSONParser();
            JSONObject obj     = (JSONObject)parser.parse(payload);
            JSONArray extensions    = (JSONArray)obj.get("contents");
            if(extensions!=null && extensions.size()>0) {
            	String self = ((JSONObject) extensions.get(0)).get("self").toString(); //This containts full URL
            	extensionId = self.split(profile.getHostUrl() + "/"+ profile.getApi_version() + "/organizations/"+ profile.getOrg() + "/environments/"+ profile.getEnvironment()+"/extensions/")[1];
            	return extensionId;
            }
        } catch (ParseException pe){
            logger.error("Get Extension parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Extension error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return null;
    }
}
