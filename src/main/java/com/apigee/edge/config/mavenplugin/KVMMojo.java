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

import com.apigee.edge.config.mavenplugin.kvm.*;
import com.apigee.edge.config.rest.RestUtil;
import com.apigee.edge.config.utils.ServerProfile;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Key;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import com.google.api.client.http.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**                                                                                                                                     ¡¡
 * Goal to create KVM in Apigee EDGE.
 * scope: org, env, api
 *
 * @author madhan.sadasivam
 * @goal kvms
 * @phase install
 */

public class KVMMojo extends GatewayAbstractMojo
{
	static Logger logger = LoggerFactory.getLogger(KVMMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;
	private Kvm kvmOrg;
	private Kvm kvmApi;
	private Kvm kvmEnv;

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
            kvmOrg = new KvmOrg();
            kvmApi = new KvmApi();
            kvmEnv = new KvmEnv();
	
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
                buildOption != OPTIONS.create && 
                buildOption != OPTIONS.delete && 
                buildOption != OPTIONS.sync) {
                return;
            }

            for (String kvm : kvms) {
                String kvmName = getKVMName(kvm);
                if (kvmName == null) {
                    throw new IllegalArgumentException(
                       "KVM does not have a name.\n" + kvm + "\n");
                }

                if (existingKVM.contains(kvmName)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Org KVM \"" + kvmName + 
                                                    "\" exists. Updating.");
                            kvmOrg.update(new KvmValueObject(serverProfile, kvmName, kvm));
                            break;
                        case create:
                            logger.info("Org KVM \"" + kvmName + 
                                                "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Org KVM \"" + kvmName + 
                                "\" already exists. Deleting.");
                            deleteOrgKVM(serverProfile, kvmName);
                            break;
                        case sync:
                            logger.info("Org KVM \"" + kvmName + 
                                "\" already exists. Deleting and recreating.");
                            deleteOrgKVM(serverProfile, kvmName);
                            logger.info("Creating Org KVM - " + kvmName);
                            createOrgKVM(serverProfile, kvm);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Org KVM - " + kvmName);
                            createOrgKVM(serverProfile, kvm);
                            break;
                        case delete:
                            logger.info("Org KVM \"" + kvmName + 
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

    protected void doEnvUpdate(List<String> kvms, String scope)
                                                 throws MojoFailureException {
        try {
            List existingKVM = getEnvKVM(serverProfile);
            if (buildOption != OPTIONS.update && 
                buildOption != OPTIONS.create && 
                buildOption != OPTIONS.delete && 
                buildOption != OPTIONS.sync) {
                return;
            }

            for (String kvm : kvms) {
                String kvmName = getKVMName(kvm);
                if (kvmName == null) {
                    throw new IllegalArgumentException(
                       "KVM does not have a name.\n" + kvm + "\n");
                }

                if (existingKVM.contains(kvmName)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Env KVM \"" + kvmName + 
                                                    "\" exists. Updating.");
                            kvmEnv.update(new KvmValueObject(serverProfile, kvmName, kvm));
                            break;
                        case create:
                            logger.info("Env KVM \"" + kvmName + 
                                                "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Env KVM \"" + kvmName + 
                                "\" already exists. Deleting.");
                            deleteEnvKVM(serverProfile, kvmName);
                            break;
                        case sync:
                            logger.info("Env KVM \"" + kvmName + 
                                "\" already exists. Deleting and recreating.");
                            deleteEnvKVM(serverProfile, kvmName);
                            logger.info("Creating Env KVM - " + kvmName);
                            createEnvKVM(serverProfile, kvm);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Env KVM - " + kvmName);
                            createEnvKVM(serverProfile, kvm);
                            break;
                        case delete:
                            logger.info("Env KVM \"" + kvmName + 
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

	protected void doAPIUpdate(String api, List<String> kvms)
                                                 throws MojoFailureException {
		try {
			List existingKVM = getAPIKVM(serverProfile, api);
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create && 
                buildOption != OPTIONS.delete && 
                buildOption != OPTIONS.sync) {
				return;
			}

	        for (String kvm : kvms) {
	        	String kvmName = getKVMName(kvm);
	        	if (kvmName == null) {
	        		throw new IllegalArgumentException(
	        		   "KVM does not have a name.\n" + kvm + "\n");
	        	}

        		if (existingKVM.contains(kvmName)) {
                    switch (buildOption) {
                        case update:
                            logger.info("API KVM \"" + kvmName + 
                                                    "\" exists. Updating.");
                            kvmApi.update(new KvmValueObject(serverProfile, api, kvmName, kvm));
                            break;
                        case create:
                            logger.info("API KVM \"" + kvmName + 
                                                "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("API KVM \"" + kvmName + 
                                            "\" already exists. Deleting.");
                            deleteAPIKVM(serverProfile, api, kvmName);
                            break;
                        case sync:
                            logger.info("API KVM \"" + kvmName + 
                                            "\" already exists. Deleting and recreating.");
                            deleteAPIKVM(serverProfile, api, kvmName);
                            logger.info("Creating API KVM - " + kvmName);
                            createAPIKVM(serverProfile, api, kvm);
                            break;
                    }
	        	} else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating API KVM - " + kvmName);
                            createAPIKVM(serverProfile, api, kvm);
                            break;
                        case delete:
                            logger.info("API KVM \"" + kvmName + 
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

		Logger logger = LoggerFactory.getLogger(KVMMojo.class);

		try {
			
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
			List kvms = getOrgConfig(logger, "kvms");
			if (kvms == null || kvms.size() == 0) {
				logger.info("No org scoped KVM config found.");
			} else {
                doOrgUpdate(kvms, scope);
            }

            /* env scoped KVMs */
            kvms = getEnvConfig(logger, "kvms");
            if (kvms == null || kvms.size() == 0) {
                logger.info("No env scoped KVM config found.");
            } else {
                doEnvUpdate(kvms, scope);
            }

            // /* API scoped KVMs */
            Set<String> apis = getAPIList(logger);
            if (apis == null || apis.size() == 0) {
                logger.info("No API scoped KVM config found.");
                return;
            }

            for (String api : apis) {
                kvms = getAPIConfig(logger, "kvms", api);
                if (kvms == null || kvms.size() == 0) {
                    logger.info(
                        "No API scoped KVM config found for " + api);
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

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createOrgKVM(ServerProfile profile, String kvm)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createOrgConfig(profile,
                                                            "keyvaluemaps", 
                                                            kvm);
        try {

            logger.debug("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteOrgKVM(ServerProfile profile, String kvmEntry)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteOrgConfig(profile,
                                                            "keyvaluemaps", 
                                                            kvmEntry);
        try {
            
            logger.debug("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getOrgKVM(ServerProfile profile)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, "keyvaluemaps");
        if(response == null) return new ArrayList();
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
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createEnvConfig(profile,
                                                    "keyvaluemaps", 
                                                    kvm);
        try {

            logger.debug("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteEnvKVM(ServerProfile profile, 
                                        String kvmEntry)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteEnvConfig(profile,
                                                    "keyvaluemaps", 
                                                    kvmEntry);
        try {
            
            logger.debug("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getEnvKVM(ServerProfile profile)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getEnvConfig(profile, "keyvaluemaps");
        if(response == null) return new ArrayList();
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
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createAPIConfig(profile,
                                                            api,
                                                            "keyvaluemaps", 
                                                            kvm);
        try {

            logger.debug("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteAPIKVM(ServerProfile profile, 
                                        String api,
                                        String kvmEntry)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteAPIConfig(profile,
                                                            api,
                                                            "keyvaluemaps", 
                                                            kvmEntry);
        try {
            
            logger.debug("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getAPIKVM(ServerProfile profile, String api)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getAPIConfig(profile, api,
                                                        "keyvaluemaps");
        if(response == null) return new ArrayList();
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




