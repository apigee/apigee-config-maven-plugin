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
 * Goal to create virtual hosts in Apigee EDGE.
 * scope: env
 *
 * @author madhan.sadasivam,waghelanikit
 * @goal virtualhosts
 * @phase verify
 */

public class VirtualHostsMojo extends GatewayAbstractMojo
{
    static Logger logger = LoggerFactory.getLogger(VirtualHostsMojo.class);
    public static final String ____ATTENTION_MARKER____ =
            "************************************************************************";

    enum OPTIONS {
        none, create, update, delete, sync
    }

    OPTIONS buildOption = OPTIONS.none;

    private ServerProfile serverProfile;

    public static class VirtualHost {
        @Key
        public String name;
    }

    public VirtualHostsMojo() {
        super();

    }

    public void init() throws MojoFailureException {
        try {
            logger.info(____ATTENTION_MARKER____);
            logger.info("Apigee Virtual Host");
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

    protected String getVHostName(String payload) throws MojoFailureException {
        Gson gson = new Gson();
        try {
            VirtualHost vHost = gson.fromJson(payload, VirtualHost.class);
            return vHost.name;
        } catch (JsonParseException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    protected void doUpdate(List<String> vHosts) throws MojoFailureException {
        try {
            List existingvHosts = null;
            if (buildOption != OPTIONS.update &&
                    buildOption != OPTIONS.create &&
                    buildOption != OPTIONS.delete &&
                    buildOption != OPTIONS.sync) {
                return;
            }

            logger.info("Retrieving existing environment Virtual Hosts - " +
                    serverProfile.getEnvironment());
            existingvHosts = getVHost(serverProfile);

            for (String vHost : vHosts) {
                String vHostName = getVHostName(vHost);
                if (vHostName == null) {
                    throw new IllegalArgumentException(
                            "Virtual Host does not have a name.\n" + vHost + "\n");
                }

                if (existingvHosts.contains(vHostName)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Virtual Host \"" + vHostName +
                                    "\" exists. Updating.");
                            updateVHost(serverProfile, vHostName, vHost);
                            break;
                        case create:
                            logger.info("Virtual Host \"" + vHostName +
                                    "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Virtual Host \"" + vHostName +
                                    "\" already exists. Deleting.");
                            deleteVHost(serverProfile, vHostName);
                            break;
                        case sync:
                            logger.info("Virtual Host \"" + vHostName +
                                    "\" already exists. Deleting and recreating.");
                            deleteVHost(serverProfile, vHostName);
                            logger.info("Creating Virtual Host - " + vHostName);
                            createVHost(serverProfile, vHost);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Virtual Host - " + vHostName);
                            createVHost(serverProfile, vHost);
                            break;
                        case delete:
                            logger.info("Virtual Host \"" + vHostName +
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

        Logger logger = LoggerFactory.getLogger(VirtualHostsMojo.class);

        try {

            init();

            if (buildOption == OPTIONS.none) {
                logger.info("Skipping Virtual Hosts (default action)");
                return;
            }

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                        "Apigee environment not found in profile");
            }

            List vHosts = getEnvConfig(logger, "virtualHosts");
            if (vHosts == null || vHosts.size() == 0) {
                logger.info(
                        "No Virtual Host config found.");
                return;
            }

            doUpdate(vHosts);

        } catch (MojoFailureException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createVHost(ServerProfile profile, String vHost)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createEnvConfig(profile,
                "virtualhosts",
                vHost);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Virtual Host create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateVHost(ServerProfile profile,
                                     String vHostName,
                                     String vHost)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.updateEnvConfig(profile,
                "virtualhosts",
                vHostName,
                vHost);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("Virtual Host update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteVHost(ServerProfile profile,
                                     String vHostName)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteEnvConfig(profile,
                "virtualhosts",
                vHostName);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Virtual Host delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getVHost(ServerProfile profile)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getEnvConfig(profile, "virtualhosts");
        if(response == null) return new ArrayList();
        JSONArray vHosts = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"vHosts\": " + payload + "}";

            JSONParser parser = new JSONParser();
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            vHosts    = (JSONArray)obj1.get("vHosts");

        } catch (ParseException pe){
            logger.error("Get Virtual Host parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get Virtual Host error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return vHosts;
    }
}
