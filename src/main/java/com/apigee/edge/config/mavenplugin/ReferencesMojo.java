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
 * Goal to create references in Apigee EDGE.
 * scope: env
 *
 * @author saisaran.vaidyanathan
 * @goal references
 * @phase verify
 */

public class ReferencesMojo extends GatewayAbstractMojo
{
    static Logger logger = LoggerFactory.getLogger(ReferencesMojo.class);
    public static final String ____ATTENTION_MARKER____ =
            "************************************************************************";

    enum OPTIONS {
        none, create, update, delete, sync
    }

    OPTIONS buildOption = OPTIONS.none;

    private ServerProfile serverProfile;

    public static class Reference {
        @Key
        public String name;
    }

    public ReferencesMojo() {
        super();

    }

    public void init() throws MojoFailureException {
        try {
            logger.info(____ATTENTION_MARKER____);
            logger.info("Apigee Reference");
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

    protected String getReferenceName(String payload) throws MojoFailureException {
        Gson gson = new Gson();
        try {
            Reference ref = gson.fromJson(payload, Reference.class);
            return ref.name;
        } catch (JsonParseException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    protected void doUpdate(List<String> refs) throws MojoFailureException {
        try {
            List existingRefs = null;
            if (buildOption != OPTIONS.update &&
                    buildOption != OPTIONS.create &&
                    buildOption != OPTIONS.delete &&
                    buildOption != OPTIONS.sync) {
                return;
            }

            logger.info("Retrieving existing environment References - " +
                    serverProfile.getEnvironment());
            existingRefs = getReferences(serverProfile);

            for (String ref : refs) {
                String refName = getReferenceName(ref);
                if (refName == null) {
                    throw new IllegalArgumentException(
                            "Reference does not have a name.\n" + ref + "\n");
                }

                if (existingRefs.contains(refName)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Reference \"" + refName +
                                    "\" exists. Updating.");
                            updateReference(serverProfile, refName, ref);
                            break;
                        case create:
                            logger.info("Reference \"" + refName +
                                    "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Reference \"" + refName +
                                    "\" already exists. Deleting.");
                            deleteReference(serverProfile, refName);
                            break;
                        case sync:
                            logger.info("Reference \"" + refName +
                                    "\" already exists. Deleting and recreating.");
                            deleteReference(serverProfile, refName);
                            logger.info("Creating Reference - " + refName);
                            createReference(serverProfile, ref);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Reference - " + refName);
                            createReference(serverProfile, ref);
                            break;
                        case delete:
                            logger.info("Reference \"" + refName +
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

        Logger logger = LoggerFactory.getLogger(ReferencesMojo.class);

        try {

            init();

            if (buildOption == OPTIONS.none) {
                logger.info("Skipping Reference (default action)");
                return;
            }

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                        "Apigee environment not found in profile");
            }

            List references = getEnvConfig(logger, "references");
            if (references == null || references.size() == 0) {
                logger.info(
                        "No References config found.");
                return;
            }

            doUpdate(references);

        } catch (MojoFailureException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createReference(ServerProfile profile, String reference)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createEnvConfig(profile,
                "references",
                reference);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Reference create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateReference(ServerProfile profile,
                                     String refName,
                                     String reference)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.updateEnvConfig(profile,
                "references",
                refName,
                reference);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("Reference update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteReference(ServerProfile profile,
                                     String refName)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteEnvConfig(profile,
                "references",
                refName);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Reference delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getReferences(ServerProfile profile)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getEnvConfig(profile, "references");
        if(response == null) return new ArrayList();
        JSONArray references = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"references\": " + payload + "}";

            JSONParser parser = new JSONParser();
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            references    = (JSONArray)obj1.get("references");

        } catch (ParseException pe){
            logger.error("Get Reference parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Reference error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return references;
    }
}
