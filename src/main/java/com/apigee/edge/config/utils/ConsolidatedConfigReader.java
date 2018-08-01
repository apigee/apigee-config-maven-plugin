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
package com.apigee.edge.config.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Read config from edge.json
 *
 * @author madhan.sadasivam
 */

public class ConsolidatedConfigReader {

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
            throws IOException {

        Logger logger = LoggerFactory.getLogger(ConfigReader.class);

        try {
            Map yaml = new ObjectMapper(new YAMLFactory()).readValue(configFile, Map.class);
            Optional<List> configs = Optional.ofNullable(yaml.get(scope))
                .map(o -> ((Map) o).get(env))
                .map(o -> (List) ((Map) o).get(resource));

            if (!configs.isPresent()) {
                return null;
            }

            ObjectMapper om = new ObjectMapper();
            List<String> out = new ArrayList<>();
            for (Object config : configs.get()) {
                out.add(om.writeValueAsString(config));
            }
            return out;
        }
        catch(IOException e) {
            logger.info(e.getMessage());
            throw e;
        }
    }

        /**
         * Example Hierarchy
         * orgConfig.apiProducts
         *
         * Returns List of
         * [ {apiProduct1}, {apiProduct2}, {apiProduct3} ]
         */
    public static List getOrgConfig(File configFile,
                                 String scope,
                                 String resource)
            throws IOException {

        Logger logger = LoggerFactory.getLogger(ConfigReader.class);

        try {
            Map yaml = new ObjectMapper(new YAMLFactory()).readValue(configFile, Map.class);
            Optional<List> configs = Optional.ofNullable(yaml.get(scope))
                .map(o -> (List) ((Map) o).get(resource));

            if (!configs.isPresent()) {
                return null;
            }

            ObjectMapper om = new ObjectMapper();
            List<String> out = new ArrayList<>();
            for (Object config : configs.get()) {
                out.add(om.writeValueAsString(config));
            }
            return out;
        }
        catch(IOException e) {
            logger.info(e.getMessage());
            throw e;
        }
    }

    /**
     * Example Hierarchy
     * orgConfig.developerApps.<developerId>.apps
     *
     * Returns Map of
     * <developerId> => [ {app1}, {app2}, {app3} ]
     */
    public static Map<String, List<String>> getOrgConfigWithId(File configFile,
                                                             String scope,
                                                             String resource)
            throws IOException {

        Logger logger = LoggerFactory.getLogger(ConfigReader.class);

        try {
            Map yaml = new ObjectMapper(new YAMLFactory()).readValue(configFile, Map.class);
            Optional<Map> map = Optional.ofNullable(yaml.get(scope))
                .map(o -> (Map) ((Map) o).get(resource));

            if (!map.isPresent()) {
                return null;
            }

            ObjectMapper om = new ObjectMapper();
            Map<String, List<String>> out = new HashMap<>();

            for (Object key : map.get().keySet()) {
                out.put((String) key, new LinkedList<>());
                for (Object v : (List) map.get().get(key)) {
                    out.get(key).add(om.writeValueAsString(v));
                }
            }
            return out;
        }
        catch(IOException ie) {
            logger.info(ie.getMessage());
            throw ie;
        }
    }

    /**
     * List of APIs under apiConfig
     */
    public static Set<String> getAPIList(File configFile)
            throws IOException {

        Logger logger = LoggerFactory.getLogger(ConfigReader.class);

        try {
            Map yaml = new ObjectMapper(new YAMLFactory()).readValue(configFile, Map.class);
            return Optional.ofNullable(yaml.get("apiConfig"))
                .map(o -> ((Map) o).keySet())
                .orElse(null);
        }
        catch(IOException ie) {
            logger.info(ie.getMessage());
            throw ie;
        }
    }

    /**
     * API Config
     * [ {apiProduct1}, {apiProduct2}, {apiProduct3} ]
     */
    public static List getAPIConfig(File configFile,
                                     String api,
                                     String resource)
            throws IOException {

        Logger logger = LoggerFactory.getLogger(ConfigReader.class);

        try {
            Map yaml = new ObjectMapper(new YAMLFactory()).readValue(configFile, Map.class);
            Optional<List> configs = Optional.ofNullable(yaml.get("apiConfig"))
                .map(o -> ((Map) o).get(api))
                .map(o -> (List) ((Map) o).get(resource));

            if (!configs.isPresent()) {
                return null;
            }

            ObjectMapper om = new ObjectMapper();
            List<String> out = new ArrayList<>();
            for (Object config : configs.get()) {
                out.add(om.writeValueAsString(config));
            }
            return out;
        }
        catch(IOException ie) {
            logger.info(ie.getMessage());
            throw ie;
        }
    }
}
