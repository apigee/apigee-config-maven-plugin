/**
 * Copyright (C) 2014 Apigee Corporation
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
package com.apigee.edge.config.utils;

import java.io.File;
import java.io.BufferedReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;

/**
 * updates the configuration values of a package
 *
 * @author madhan.sadasivam
 */

public class ConfigReader {

    /**
     * Example Hierarchy
     * envConfig.cache.<env>.caches
     * 
     * Returns List of
     * [ {cache1}, {cache2}, {cache3} ]
     */
    public static List getEnvConfig(String env, 
                                     File configFile, 
                                     String scope,
                                     String resource)
            throws ParseException, IOException {

        Logger logger = LoggerFactory.getLogger(ConfigReader.class);

        JSONParser parser = new JSONParser();
        ArrayList out = null;    
        try {
            BufferedReader bufferedReader = new BufferedReader(
                new java.io.FileReader(configFile));

            JSONObject edgeConf     = (JSONObject)parser.parse(bufferedReader);
            if (edgeConf == null) return null;

            JSONObject scopeConf  = (JSONObject)edgeConf.get(scope);
            if (scopeConf == null) return null;

            JSONObject envConf  = (JSONObject)scopeConf.get(env);
            if (envConf == null) return null;

            JSONArray  configs      = (JSONArray)envConf.get(resource);
            if (configs == null) return null;

            out = new ArrayList();
            for (Object config: configs) {              
                out.add(((JSONObject)config).toJSONString());
            }
        }
        catch(IOException ie) {
            logger.info(ie.getMessage());
            throw ie;
        }
        catch(ParseException pe) {
            logger.info(pe.getMessage());
            throw pe;
        }

        return out;
    }

    /**
     * Example Hierarchy
     * orgConfig.apiProducts
     * 
     * Returns List of
     * [ {apiProduct1}, {apiProduct2}, {apiProduct3} ]
     */
    public static List getConfig(String env, 
                                 File configFile, 
                                 String scope,
                                 String resource)
            throws ParseException, IOException {

        Logger logger = LoggerFactory.getLogger(ConfigReader.class);

        JSONParser parser = new JSONParser();
        ArrayList out = null;    
        try {
            BufferedReader bufferedReader = new BufferedReader(
                new java.io.FileReader(configFile));

            JSONObject edgeConf     = (JSONObject)parser.parse(bufferedReader);
            if (edgeConf == null) return null;

            JSONObject scopeConf  = (JSONObject)edgeConf.get(scope);
            if (scopeConf == null) return null;

            JSONArray  configs      = (JSONArray)scopeConf.get(resource);
            if (configs == null) return null;

            out = new ArrayList();
            for (Object config: configs) {              
                out.add(((JSONObject)config).toJSONString());
            }
        }
        catch(IOException ie) {
            logger.info(ie.getMessage());
            throw ie;
        }
        catch(ParseException pe) {
            logger.info(pe.getMessage());
            throw pe;
        }

        return out;
    }

    /**
     * Example Hierarchy
     * orgConfig.developerApps.<developerId>.apps
     * 
     * Returns Map of
     * <developerId> => [ {app1}, {app2}, {app3} ]
     */
    public static Map<String, List<String>> getConfigWithId(String env, 
                                                             File configFile, 
                                                             String scope,
                                                             String resource)
            throws ParseException, IOException {

        Logger logger = LoggerFactory.getLogger(ConfigReader.class);

        JSONParser parser = new JSONParser();
        HashMap <String, List<String>> out = null;    
        List<String> outStrs = null;
        try {
            BufferedReader bufferedReader = new BufferedReader(
                new java.io.FileReader(configFile));

            JSONObject edgeConf     = (JSONObject)parser.parse(bufferedReader);
            if (edgeConf == null) return null;

            // orgConfig
            JSONObject scopeConf  = (JSONObject)edgeConf.get(scope);
            if (scopeConf == null) return null;

            // orgConfig.developerApps
            HashMap  sConfig      = (HashMap)scopeConf.get(resource);
            if (sConfig == null) return null;

            // orgConfig.developerApps.<developerId>
            Iterator it = sConfig.entrySet().iterator();
            out = new HashMap<String, List<String>> ();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                JSONArray confs = (JSONArray)pair.getValue();
                outStrs = new ArrayList<String>();
                for (Object conf: confs) {              
                    outStrs.add(((JSONObject)conf).toJSONString());
                }
                out.put((String)pair.getKey(), outStrs);
            }

        }
        catch(IOException ie) {
            logger.info(ie.getMessage());
            throw ie;
        }
        catch(ParseException pe) {
            logger.info(pe.getMessage());
            throw pe;
        }

        return out;
    }

}
