package com.apigee.edge.config.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read config from resources/edge/.../*.json files
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
    public static List getEnvConfig(String env, File configFile) throws ParseException, IOException {
        return getListConfig(configFile);
    }

    /**
     * Example hierarchy orgConfig.apiProducts
     *
     * @return List of JSON strings, e.g. [ {apiProduct1}, {apiProduct2}, {apiProduct3} ]
     */

    // TODO convert parse exception error message more human friendly
    public static List getOrgConfig(File configFile) throws ParseException, IOException {
        return getListConfig(configFile);
    }

    private static List getListConfig(File configFile) throws IOException, ParseException {
        Logger logger = LoggerFactory.getLogger(ConfigReader.class);
        try {
            if (configFile.getName().endsWith(".yaml")) {
                return getOrgConfigFromYaml(configFile);
            }
            else {
                return getOrgConfigFromJson(configFile);
            }
        }
        catch(IOException | ParseException ie) {
            logger.info(ie.getMessage());
            throw ie;
        }
    }

    private static List<String> getOrgConfigFromJson(File file) throws IOException, ParseException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        JSONParser parser = new JSONParser();
        JSONArray configs = (JSONArray) parser.parse(bufferedReader);
        if (configs == null) {
            return null;
        }

        List<String> out = new ArrayList<>();
        for (Object config: configs) {
            out.add(((JSONObject)config).toJSONString());
        }
        return out;
    }

    private static List<String> getOrgConfigFromYaml(File configFile) throws IOException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        ObjectMapper om = new ObjectMapper();
        List<String> out = new ArrayList<>();
        for (Object config : yamlReader.readValue(configFile, List.class)) {
            out.add(om.writeValueAsString(config));
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
    public static Map<String, List<String>> getOrgConfigWithId(File configFile) throws ParseException, IOException {
        Logger logger = LoggerFactory.getLogger(ConfigReader.class);

        try {
            if (configFile.getName().endsWith(".yaml")) {
                return getOrgConfigWithIdFromYaml(configFile);
            }
            else {
                return getOrgConfigWithIdFromJson(configFile);
            }
        }
        catch(IOException | ParseException ie) {
            logger.info(ie.getMessage());
            throw ie;
        }
    }

    private static Map<String, List<String>> getOrgConfigWithIdFromJson(File configFile) throws IOException, ParseException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile));
        Map<?, ?> sConfig = (Map) new JSONParser().parse(bufferedReader);
        if (sConfig == null) {
            return null;
        }

        // orgConfig.developerApps.<developerId>
        Map <String, List<String>> out = new HashMap<>();
        for (Object key : sConfig.keySet()) {
            out.put((String) key, new LinkedList<>());
            for (Object conf : (JSONArray) sConfig.get(key)) {
                out.get(key).add(((JSONObject) conf).toJSONString());
            }
        }
        return out;
    }

    private static Map<String, List<String>> getOrgConfigWithIdFromYaml(File configFile) throws IOException {
        ObjectMapper om = new ObjectMapper();
        Map<String, List<String>> out = new HashMap<>();

        Map map = new ObjectMapper(new YAMLFactory()).readValue(configFile, Map.class);
        for (Object key : map.keySet()) {
            out.put((String) key, new LinkedList<>());
            for (Object v : (List) map.get(key)) {
                out.get(key).add(om.writeValueAsString(v));
            }
        }
        return out;
    }

    /**
     * List of APIs under configDir/api
     */
    public static Set<String> getAPIList(String apiConfigDir) {
        File[] baseDir = Optional.ofNullable(new File(apiConfigDir).listFiles())
                .orElse(new File[0]);

        Set<String> apis = Arrays.stream(baseDir)
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toSet());

        return apis.isEmpty() ? null : apis;
    }

    /**
     * API Config
     * [ {apiProduct1}, {apiProduct2}, {apiProduct3} ]
     */
    public static List getAPIConfig(File configFile) throws ParseException, IOException {
        return getListConfig(configFile);
    }
}
