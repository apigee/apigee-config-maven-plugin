package com.apigee.edge.config.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConsolidatedConfigReaderTest {

    private final Path basePath = Paths.get("src/test/resources", getClass().getName());

    @Test
    void getEnvConfigWithMissingFile() {
        Path input = basePath.resolve("getEnvConfigWithMissingFile-input.json");
        Executable getEnvConfig = () -> ConsolidatedConfigReader.getEnvConfig("dev", input.toFile(), "envConfig", "caches");
        assertThrows(IOException.class, getEnvConfig);
    }

    @Test
    void getEnvConfigWithMissingFileFromYaml() {
        Path input = basePath.resolve("getEnvConfigWithMissingFileFromYaml-input.yaml");
        Executable getEnvConfig = () -> ConsolidatedConfigReader.getEnvConfig("dev", input.toFile(), "envConfig", "caches");
        assertThrows(IOException.class, getEnvConfig);
    }

    @Test
    void getEnvConfig() throws IOException, ParseException {
        Path input = basePath.resolve("getEnvConfig-input.json");
        List actual = ConsolidatedConfigReader.getEnvConfig("test", input.toFile(), "envConfig", "caches");

        assertNotNull(actual);
        assertEquals(2, actual.size());

        ObjectMapper om = new ObjectMapper();
        Map actual0 = om.readValue((String) actual.get(0), Map.class);
        Map expected0 = om.readValue(basePath.resolve("getEnvConfig-expected0.json").toFile(), Map.class);
        assertEquals(actual0, expected0);

        Map actual1 = om.readValue((String) actual.get(1), Map.class);
        Map expected1 = om.readValue(basePath.resolve("getEnvConfig-expected1.json").toFile(), Map.class);
        assertEquals(actual1, expected1);
    }

    @Test
    void getEnvConfigFromYaml() throws IOException, ParseException {
        Path input = basePath.resolve("getEnvConfigFromYaml-input.yaml");
        List actual = ConsolidatedConfigReader.getEnvConfig("test", input.toFile(), "envConfig", "caches");

        assertNotNull(actual);
        assertEquals(2, actual.size());

        ObjectMapper om = new ObjectMapper();
        Map actual0 = om.readValue((String) actual.get(0), Map.class);
        Map expected0 = om.readValue(basePath.resolve("getEnvConfigFromYaml-expected0.json").toFile(), Map.class);
        assertEquals(actual0, expected0);

        Map actual1 = om.readValue((String) actual.get(1), Map.class);
        Map expected1 = om.readValue(basePath.resolve("getEnvConfigFromYaml-expected1.json").toFile(), Map.class);
        assertEquals(actual1, expected1);
    }

    @Test
    void getOrgConfigWithMissingFile() {
        Path input = basePath.resolve("getOrgConfigWithMissingFile-input.json");
        Executable getOrgConfig = () -> ConsolidatedConfigReader.getOrgConfig(input.toFile(), "orgConfig", "apiProducts");
        assertThrows(IOException.class, getOrgConfig);
    }

    @Test
    void getOrgConfigWithMissingFileFromYaml() {
        Path input = basePath.resolve("getOrgConfigWithMissingFileFromYaml-input.yaml");
        Executable getOrgConfig = () -> ConsolidatedConfigReader.getOrgConfig(input.toFile(), "orgConfig", "apiProducts");
        assertThrows(IOException.class, getOrgConfig);
    }

    @Test
    void getOrgConfig() throws IOException, ParseException {
        Path input = basePath.resolve("getOrgConfig-input.json");
        List actual = ConsolidatedConfigReader.getOrgConfig(input.toFile(), "orgConfig", "apiProducts");

        assertNotNull(actual);
        assertEquals(2, actual.size());

        ObjectMapper om = new ObjectMapper();
        Map actual0 = om.readValue((String) actual.get(0), Map.class);
        Map expected0 = om.readValue(basePath.resolve("getOrgConfig-expected0.json").toFile(), Map.class);
        assertEquals(actual0, expected0);

        Map actual1 = om.readValue((String) actual.get(1), Map.class);
        Map expected1 = om.readValue(basePath.resolve("getOrgConfig-expected1.json").toFile(), Map.class);
        assertEquals(actual1, expected1);
    }

    @Test
    void getOrgConfigFromYaml() throws IOException, ParseException {
        Path input = basePath.resolve("getOrgConfigFromYaml-input.yaml");
        List actual = ConsolidatedConfigReader.getOrgConfig(input.toFile(), "orgConfig", "apiProducts");

        assertNotNull(actual);
        assertEquals(2, actual.size());

        ObjectMapper om = new ObjectMapper();
        Map actual0 = om.readValue((String) actual.get(0), Map.class);
        Map expected0 = om.readValue(basePath.resolve("getOrgConfigFromYaml-expected0.json").toFile(), Map.class);
        assertEquals(actual0, expected0);

        Map actual1 = om.readValue((String) actual.get(1), Map.class);
        Map expected1 = om.readValue(basePath.resolve("getOrgConfigFromYaml-expected1.json").toFile(), Map.class);
        assertEquals(actual1, expected1);
    }

    @Test
    void getOrgConfigWithIdWithMissingFile() {
        Path input = basePath.resolve("getOrgConfigWithIdWithMissingFile-input.json");
        Executable getOrgConfigWithId = () -> ConsolidatedConfigReader.getOrgConfigWithId(input.toFile(), "orgConfig", "developerApps");
        assertThrows(IOException.class, getOrgConfigWithId);
    }

    @Test
    void getOrgConfigWithIdWithMissingFileFromYaml() {
        Path input = basePath.resolve("getOrgConfigWithIdWithMissingFileFromYaml-input.yaml");
        Executable getOrgConfigWithId = () -> ConsolidatedConfigReader.getOrgConfigWithId(input.toFile(), "orgConfig", "developerApps");
        assertThrows(IOException.class, getOrgConfigWithId);
    }

    @Test
    void getOrgConfigWithId() throws IOException, ParseException {
        Path input = basePath.resolve("getOrgConfigWithId-input.json");
        Map<String, List<String>> actual = ConsolidatedConfigReader.getOrgConfigWithId(input.toFile(), "orgConfig", "developerApps");

        assertNotNull(actual);
        assertAll(
                () -> assertEquals(2, actual.size()),
                () -> assertTrue(actual.containsKey("grant@enterprise.com")),
                () -> assertTrue(actual.containsKey("hugh@example.com"))
        );

        // Developer 1
        ObjectMapper om = new ObjectMapper();
        Assert.assertNotNull(actual.get("grant@enterprise.com"));
        List<Map> actual0 = new LinkedList<>();
        for (Object s : actual.get("grant@enterprise.com")) {
            actual0.add(om.readValue((String) s, Map.class));
        }
        List expected0 = om.readValue(basePath.resolve("getOrgConfigWithId-expected0.json").toFile(), List.class);
        assertEquals(expected0, actual0);

        // Developer 2
        Assert.assertNotNull(actual.get("hugh@example.com"));
        List<Map> actual1 = new LinkedList<>();
        for (Object s : actual.get("hugh@example.com")) {
            actual1.add(om.readValue((String) s, Map.class));
        }
        List expected1 = om.readValue(basePath.resolve("getOrgConfigWithId-expected1.json").toFile(), List.class);
        assertEquals(expected1, actual1);
    }

    @Test
    void getOrgConfigWithIdFromYaml() throws IOException, ParseException {
        Path input = basePath.resolve("getOrgConfigWithIdFromYaml-input.yaml");
        Map<String, List<String>> actual = ConsolidatedConfigReader.getOrgConfigWithId(input.toFile(), "orgConfig", "developerApps");

        assertNotNull(actual);
        assertAll(
                () -> assertEquals(2, actual.size()),
                () -> assertTrue(actual.containsKey("grant@enterprise.com")),
                () -> assertTrue(actual.containsKey("hugh@example.com"))
        );

        // Developer 1
        ObjectMapper om = new ObjectMapper();
        Assert.assertNotNull(actual.get("grant@enterprise.com"));
        List<Map> actual0 = new LinkedList<>();
        for (Object s : actual.get("grant@enterprise.com")) {
            actual0.add(om.readValue((String) s, Map.class));
        }
        List expected0 = om.readValue(basePath.resolve("getOrgConfigWithIdFromYaml-expected0.json").toFile(), List.class);
        assertEquals(expected0, actual0);

        // Developer 2
        Assert.assertNotNull(actual.get("hugh@example.com"));
        List<Map> actual1 = new LinkedList<>();
        for (Object s : actual.get("hugh@example.com")) {
            actual1.add(om.readValue((String) s, Map.class));
        }
        List expected1 = om.readValue(basePath.resolve("getOrgConfigWithIdFromYaml-expected1.json").toFile(), List.class);
        assertEquals(expected1, actual1);
    }

    @Test
    void getAPIListWithMissingFile() {
        Path input = basePath.resolve("getAPIListWithMissingFile-input.json");
        Executable getAPIList = () -> ConsolidatedConfigReader.getAPIList(input.toFile());
        assertThrows(IOException.class, getAPIList);
    }

    @Test
    void getAPIListWithMissingFileFromYaml() {
        Path input = basePath.resolve("getAPIListWithMissingFileFromYaml-input.yaml");
        Executable getAPIList = () -> ConsolidatedConfigReader.getAPIList(input.toFile());
        assertThrows(IOException.class, getAPIList);
    }

    @Test
    void getAPIList() throws IOException, ParseException {
        Path input = basePath.resolve("getAPIList-input.json");
        Set<String> actual = ConsolidatedConfigReader.getAPIList(input.toFile());

        assertNotNull(actual);
        assertAll(
                () -> assertEquals(2, actual.size()),
                () -> assertTrue(actual.contains("forecastweatherapi")),
                () -> assertTrue(actual.contains("myotherapi"))
        );
    }

    @Test
    void getAPIListFromYaml() throws IOException, ParseException {
        Path input = basePath.resolve("getAPIListFromYaml-input.yaml");
        Set<String> actual = ConsolidatedConfigReader.getAPIList(input.toFile());

        assertNotNull(actual);
        assertAll(
                () -> assertEquals(2, actual.size()),
                () -> assertTrue(actual.contains("forecastweatherapi")),
                () -> assertTrue(actual.contains("myotherapi"))
        );
    }

    @Test
    void getAPIConfigWithMissingFile() {
        Path input = basePath.resolve("getOrgConfigWithIdWithMissingFile-input.json");
        Executable getAPIConfig = () -> ConsolidatedConfigReader.getAPIConfig(input.toFile(), "forecastweatherapi", "kvms");
        assertThrows(IOException.class, getAPIConfig);
    }

    @Test
    void getAPIConfigWithMissingFileFromYaml() {
        Path input = basePath.resolve("getOrgConfigWithIdWithMissingFileFromYaml-input.yaml");
        Executable getAPIConfig = () -> ConsolidatedConfigReader.getAPIConfig(input.toFile(), "forecastweatherapi", "kvms");
        assertThrows(IOException.class, getAPIConfig);
    }

    @Test
    void getAPIConfig() throws IOException, ParseException {
        Path input = basePath.resolve("getAPIConfig-input.json");
        List actual = ConsolidatedConfigReader.getAPIConfig(input.toFile(), "forecastweatherapi", "kvms");

        assertNotNull(actual);
        assertEquals(2, actual.size());

        ObjectMapper om = new ObjectMapper();
        List<Map> actual0 = new LinkedList<>();
        for (Object o : actual) {
            actual0.add(om.readValue((String) o, Map.class));
        }
        List expected = om.readValue(basePath.resolve("getAPIConfig-expected.json").toFile(), List.class);
        assertEquals(expected, actual0);
    }

    @Test
    void getAPIConfigFromYaml() throws IOException, ParseException {
        Path input = basePath.resolve("getAPIConfigFromYaml-input.yaml");
        List actual = ConsolidatedConfigReader.getAPIConfig(input.toFile(), "forecastweatherapi", "kvms");

        assertNotNull(actual);
        assertEquals(2, actual.size());

        ObjectMapper om = new ObjectMapper();
        List<Map> actual0 = new LinkedList<>();
        for (Object o : actual) {
            actual0.add(om.readValue((String) o, Map.class));
        }
        List expected = om.readValue(basePath.resolve("getAPIConfigFromYaml-expected.json").toFile(), List.class);
        assertEquals(expected, actual0);
    }
}