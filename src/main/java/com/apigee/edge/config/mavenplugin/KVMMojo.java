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

import com.apigee.edge.config.utils.ConfigReader;
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

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.api.client.http.*;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**                                                                                                                                     ¡¡
 * Goal to create KVM in Apigee EDGE.
 * scope: env
 *
 * @author madhan.sadasivam
 * @goal keyvaluemaps
 * @phase install
 */

public class KVMMojo extends GatewayAbstractMojo
{
	static Logger logger = LoggerFactory.getLogger(KVMMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class KVM {
        @Key
        public String name;
    }
	
	public KVMMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {

		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee KVM");
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

	protected String getKVMName(String payload) throws MojoFailureException {
		Gson gson = new Gson();
		try {
			KVM kvm = gson.fromJson(payload, KVM.class);
			return kvm.name;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

    protected void doOrgUpdate(List<String> kvms, String scope)
                                                 throws MojoFailureException {
        try {
            List existingKVM = getOrgKVM(serverProfile);
            if (buildOption != OPTIONS.update && 
                buildOption != OPTIONS.create) {
                return;
            }

            for (String kvm : kvms) {
                String kvmName = getKVMName(kvm);
                if (kvmName == null) {
                    throw new IllegalArgumentException(
                       "KVM does not have a name.\n" + kvm + "\n");
                }

                if (existingKVM.contains(kvmName)) {
                    if (buildOption == OPTIONS.update) {
                        logger.info("Org KVM \"" + kvmName + 
                                                "\" exists. Updating.");
                        updateOrgKVM(serverProfile, kvmName, kvm);
                    } else {
                        logger.info("Org KVM \"" + kvmName + 
                                                "\" already exists. Skipping.");
                    }
                } else {
                    logger.info("Creating Org KVM - " + kvmName);
                    createOrgKVM(serverProfile, kvm);
                }
            }
        
        } catch (IOException e) {
            throw new MojoFailureException("Apigee network call error " +
                                                         e.getMessage());
        } catch (RuntimeException e) {
            throw e;
        }
    }

    protected void doEnvUpdate(List<String> kvms, String scope)
                                                 throws MojoFailureException {
        try {
            List existingKVM = getEnvKVM(serverProfile);
            if (buildOption != OPTIONS.update && 
                buildOption != OPTIONS.create) {
                return;
            }

            for (String kvm : kvms) {
                String kvmName = getKVMName(kvm);
                if (kvmName == null) {
                    throw new IllegalArgumentException(
                       "KVM does not have a name.\n" + kvm + "\n");
                }

                if (existingKVM.contains(kvmName)) {
                    if (buildOption == OPTIONS.update) {
                        logger.info("Env KVM \"" + kvmName + 
                                                "\" exists. Updating.");
                        updateEnvKVM(serverProfile, kvmName, kvm);
                    } else {
                        logger.info("Env KVM \"" + kvmName + 
                                                "\" already exists. Skipping.");
                    }
                } else {
                    logger.info("Creating Env KVM - " + kvmName);
                    createEnvKVM(serverProfile, kvm);
                }
            }
        
        } catch (IOException e) {
            throw new MojoFailureException("Apigee network call error " +
                                                         e.getMessage());
        } catch (RuntimeException e) {
            throw e;
        }
    }

	protected void doAPIUpdate(String api, List<String> kvms)
                                                 throws MojoFailureException {
		try {
			List existingKVM = getAPIKVM(serverProfile, api);
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create) {
				return;
			}

	        for (String kvm : kvms) {
	        	String kvmName = getKVMName(kvm);
	        	if (kvmName == null) {
	        		throw new IllegalArgumentException(
	        		   "KVM does not have a name.\n" + kvm + "\n");
	        	}

        		if (existingKVM.contains(kvmName)) {
		        	if (buildOption == OPTIONS.update) {
						logger.info("API KVM \"" + kvmName + 
												"\" exists. Updating.");
						updateAPIKVM(serverProfile, api, kvmName, kvm);
	        		} else {
	        			logger.info("API KVM \"" + kvmName + 
	        									"\" already exists. Skipping.");
	        		}
	        	} else {
					logger.info("Creating API KVM - " + kvmName);
					createAPIKVM(serverProfile, api, kvm);
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

		Logger logger = LoggerFactory.getLogger(KVMMojo.class);
		File configFile = findConfigFile(logger);
		if (configFile == null) {
			return;
		}

		try {
			fixOSXNonProxyHosts();
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping KVM (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

            /* org scoped KVMs */
            String scope = "orgConfig";
			List kvms = getOrgConfig(logger, configFile, scope);
			if (kvms == null || kvms.size() == 0) {
				logger.info("No org scoped KVM config found in edge.json.");
			} else {
                doOrgUpdate(kvms, scope);
            }

            /* env scoped KVMs */
            scope = "envConfig";
            kvms = getEnvConfig(logger, configFile, scope);
            if (kvms == null || kvms.size() == 0) {
                logger.info("No env scoped KVM config found in edge.json.");
            } else {
                doEnvUpdate(kvms, scope);
            }

            // /* API scoped KVMs */
            Set<String> apis = getAPIList(logger, configFile);
            for (String api : apis) {
                logger.info(api);
                kvms = getAPIConfig(logger, configFile, api);
                if (kvms == null || kvms.size() == 0) {
                    logger.info(
                        "No API scoped KVM config found in edge.json.");
                } else {
                    doAPIUpdate(api, kvms);
                }
            }
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    private Set<String> getAPIList(Logger logger, File configFile) 
            throws MojoExecutionException {
        logger.debug("Retrieving list of APIs from edge.json");
        try {
            return ConfigReader.getAPIList(configFile);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    private List getAPIConfig(Logger logger, File configFile, String api) 
            throws MojoExecutionException {
        logger.debug("Retrieving config from edge.json for API " + api);
        try {
            return ConfigReader.getAPIConfig(serverProfile.getEnvironment(), 
                                                configFile,
                                                api,
                                                "kvms");
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

	private List getEnvConfig(Logger logger, File configFile, String scope) 
			throws MojoExecutionException {
		logger.debug("Retrieving config from edge.json");
		try {
            return ConfigReader.getEnvConfig(serverProfile.getEnvironment(), 
                                                configFile,
                                                scope,
                                                "kvms");
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

    private List getOrgConfig(Logger logger, File configFile, String scope) 
            throws MojoExecutionException {
        logger.debug("Retrieving config from edge.json");
        try {
            return ConfigReader.getConfig(serverProfile.getEnvironment(), 
                                                configFile,
                                                scope,
                                                "kvms");
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

	private File findConfigFile(Logger logger) throws MojoExecutionException {
		File configFile = new File(super.getBaseDirectoryPath() + 
									File.separator + "edge.json");

		if (configFile.exists()) {
			return configFile;
		}

		logger.info("No edge.json found.");
		return null;
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createOrgKVM(ServerProfile profile, String kvm)
            throws IOException {

        HttpResponse response = RestUtil.createOrgConfig(profile, 
                                                            "keyvaluemaps", 
                                                            kvm);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateOrgKVM(ServerProfile profile, 
                                        String kvmEntry, 
                                        String kvm)
            throws IOException {

        HttpResponse response = RestUtil.updateOrgConfig(profile, 
                                                            "keyvaluemaps", 
                                                            kvmEntry,
                                                            kvm);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getOrgKVM(ServerProfile profile)
            throws IOException {

        HttpResponse response = RestUtil.getOrgConfig(profile, "keyvaluemaps");
        JSONArray kvms = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"kvms\": " + payload + "}";

            JSONParser parser = new JSONParser();                
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            kvms    = (JSONArray)obj1.get("kvms");

        } catch (ParseException pe){
            logger.error("Get KVM parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get KVM error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return kvms;
    }   

    public static String createEnvKVM(ServerProfile profile, String kvm)
            throws IOException {

        HttpResponse response = RestUtil.createEnvConfig(profile, 
                                                    "keyvaluemaps", 
                                                    kvm);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateEnvKVM(ServerProfile profile, 
                                        String kvmEntry, 
                                        String kvm)
            throws IOException {

        HttpResponse response = RestUtil.updateEnvConfig(profile, 
                                                    "keyvaluemaps", 
                                                    kvmEntry,
                                                    kvm);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }


    public static List getEnvKVM(ServerProfile profile)
            throws IOException {

        HttpResponse response = RestUtil.getEnvConfig(profile, "keyvaluemaps");
        JSONArray kvms = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"kvms\": " + payload + "}";

            JSONParser parser = new JSONParser();                
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            kvms    = (JSONArray)obj1.get("kvms");

        } catch (ParseException pe){
            logger.error("Get KVM parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get KVM error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return kvms;
    }   

    public static String createAPIKVM(ServerProfile profile, 
                                        String api,
                                        String kvm)
            throws IOException {

        HttpResponse response = RestUtil.createAPIConfig(profile, 
                                                            api,
                                                            "keyvaluemaps", 
                                                            kvm);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateAPIKVM(ServerProfile profile, 
                                        String api,
                                        String kvmEntry, 
                                        String kvm)
            throws IOException {

        HttpResponse response = RestUtil.updateAPIConfig(profile, 
                                                            api,
                                                            "keyvaluemaps", 
                                                            kvmEntry,
                                                            kvm);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getAPIKVM(ServerProfile profile, String api)
            throws IOException {

        HttpResponse response = RestUtil.getAPIConfig(profile, 
                                                        api, 
                                                        "keyvaluemaps");
        JSONArray kvms = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"kvms\": " + payload + "}";

            JSONParser parser = new JSONParser();                
            JSONObject obj1     = (JSONObject)parser.parse(obj);
            kvms    = (JSONArray)obj1.get("kvms");

        } catch (ParseException pe){
            logger.error("Get KVM parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get KVM error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return kvms;
    }   

}




