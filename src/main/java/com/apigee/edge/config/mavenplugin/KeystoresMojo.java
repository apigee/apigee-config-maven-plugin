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
 * Goal to create keystores in Apigee EDGE.
 * scope: env
 *
 * @author ahmed.fakhri
 * @goal keystores
 * @phase install
 */

public class KeystoresMojo extends GatewayAbstractMojo
{
    static Logger logger = LoggerFactory.getLogger(KeystoresMojo.class);
    public static final String ____ATTENTION_MARKER____ =
            "************************************************************************";

    enum OPTIONS {
        none, create, update, delete, sync
    }

    OPTIONS buildOption = OPTIONS.none;

    private ServerProfile serverProfile;

    public static class Keystore {
        @Key
        public String name;
    }

    public KeystoresMojo() {
        super();

    }

    public void init() throws MojoFailureException {
        try {
            logger.info(____ATTENTION_MARKER____);
            logger.info("Apigee Keystore");
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

    protected String getKeystoreName(String payload) throws MojoFailureException {
        Gson gson = new Gson();
        try {
            Keystore keystor = gson.fromJson(payload, Keystore.class);
            return keystor.name;
        } catch (JsonParseException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    protected void doExecute(List<String> keystores) throws MojoFailureException {
        try {
            List existingKeystores = null;
            if (buildOption != OPTIONS.update &&
                    buildOption != OPTIONS.create &&
                    buildOption != OPTIONS.delete &&
                    buildOption != OPTIONS.sync) {
                return;
            }

            logger.info("Retrieving existing environment Keystores - " +
                    serverProfile.getEnvironment());
            existingKeystores = getKeystores(serverProfile);

            for (String keystore : keystores) {
                String keystoreName = getKeystoreName(keystore);
                if (keystoreName == null) {
                    throw new IllegalArgumentException(
                            "Keystore does not have a name.\n" + keystore + "\n");
                }

                if (existingKeystores.contains(keystoreName)) {
                    switch (buildOption) {
                        case update:
                        	logger.info("Keystores cannot be updated. Please use 'sync' option if needed. Skipping.");
                            break;
                        case create:
                            logger.info("Keystore \"" + keystoreName +
                                    "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Keystore \"" + keystoreName +
                                    "\" already exists. Deleting.");
                            deleteKeystore(serverProfile, keystoreName);
                            break;
                        case sync:
                        	logger.info("Keystore \"" + keystoreName + 
                                    "\" already exists. Deleting and recreating.");
                        	deleteKeystore(serverProfile, keystoreName);
			                logger.info("Creating Cache - " + keystoreName);
			                createKeystore(serverProfile, keystore);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Keystore - " + keystoreName);
                            createKeystore(serverProfile, keystore);
                            break;
                        case delete:
                            logger.info("Keystore \"" + keystoreName +
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

        Logger logger = LoggerFactory.getLogger(KeystoresMojo.class);

        try {

            init();

            if (buildOption == OPTIONS.none) {
                logger.info("Skipping Keystore (default action)");
                return;
            }

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                        "Apigee environment not found in profile");
            }

            List keystores = getEnvConfig(logger, "keystores");
            if (keystores == null || keystores.size() == 0) {
                logger.info(
                        "No Keystores config found.");
                return;
            }

            doExecute(keystores);

        } catch (MojoFailureException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createKeystore(ServerProfile profile, String keystore)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createEnvConfig(profile,
                "keystores",
                keystore);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Keystore create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteKeystore(ServerProfile profile,
                                     String keystoreName)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteEnvConfig(profile,
                "keystores",
                keystoreName);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Keystore delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getKeystores(ServerProfile profile)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getEnvConfig(profile, "keystores");
        if(response == null) return new ArrayList();
        JSONArray keystores = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String tweekedPayload = "{ \"keystores\": " + payload + "}";

            JSONParser parser = new JSONParser();
            JSONObject jsonObject     = (JSONObject)parser.parse(tweekedPayload);
            keystores    = (JSONArray)jsonObject.get("keystores");

        } catch (ParseException pe){
            logger.error("Get Keystore parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Keystore error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return keystores;
    }
}