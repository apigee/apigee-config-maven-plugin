package com.apigee.edge.config.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigReaderTest {

    private final Path basePath = Paths.get("src/test/resources", getClass().getName());

    @Test(expected = IOException.class)
    public void testGetEnvConfigWithMissingFile() throws IOException {
        Path input = basePath.resolve("testGetEnvConfigWithMissingFile-input.json");
        ConfigReader.getOrgConfig(input.toFile());
    }

    @Test
    public void testGetEnvConfigTargetServers() throws IOException {
        Path input = basePath.resolve("testGetEnvConfigTargetServers-input.json");
        List actual = ConfigReader.getEnvConfig("dev", input.toFile());
        Assert.assertNotNull(actual);
        Assert.assertEquals(2, actual.size());

        ObjectMapper om = new ObjectMapper();
        Map actual1 = om.readValue((String) actual.get(0), Map.class);
        Map expected1 = om.readValue(basePath.resolve("testGetEnvConfigTargetServers-expected.json").toFile(), Map.class);
        Assert.assertEquals(expected1, actual1);

        Map actual2 = om.readValue((String) actual.get(1), Map.class);
        Assert.assertEquals("ESBTarget", actual2.get("name"));
        Assert.assertEquals("enterprise.com", actual2.get("host"));
        Assert.assertEquals(true, actual2.get("isEnabled"));
        Assert.assertEquals(8080, actual2.get("port"));
        Assert.assertNotNull(actual2.get("sSLInfo"));
        Assert.assertTrue(actual2.get("sSLInfo") instanceof Map);
        Map sslInfo = (Map) actual2.get("sSLInfo");
        Assert.assertEquals("false", sslInfo.get("clientAuthEnabled"));
        Assert.assertEquals("true", sslInfo.get("enabled"));
        Assert.assertEquals("false", sslInfo.get("ignoreValidationErrors"));
        Assert.assertEquals("key_alias", sslInfo.get("keyAlias"));
        Assert.assertEquals("keystore_name", sslInfo.get("keyStore"));
        Assert.assertEquals("truststore_name", sslInfo.get("trustStore"));
    }

    @Test(expected = IOException.class)
    public void testGetEnvConfigWithMissingYamlFile() throws IOException {
        Path input = basePath.resolve("testGetEnvConfigWithMissingYamlFile-input.yaml");
        ConfigReader.getOrgConfig(input.toFile());
    }

    @Test
    public void testGetEnvConfigTargetServersFromYaml() throws IOException {
        Path input = basePath.resolve("testGetEnvConfigTargetServersFromYaml-input.yaml");
        List actual = ConfigReader.getEnvConfig("dev", input.toFile());
        Assert.assertNotNull(actual);
        Assert.assertEquals(2, actual.size());

        ObjectMapper om = new ObjectMapper();
        Map actual1 = om.readValue((String) actual.get(0), Map.class);
        Map expected1 = om.readValue(basePath.resolve("testGetEnvConfigTargetServersFromYaml-expected.json").toFile(), Map.class);
        Assert.assertEquals(expected1, actual1);

        Map actual2 = om.readValue((String) actual.get(1), Map.class);
        Assert.assertEquals("ESBTarget", actual2.get("name"));
        Assert.assertEquals("enterprise.com", actual2.get("host"));
        Assert.assertEquals(true, actual2.get("isEnabled"));
        Assert.assertEquals(8080, actual2.get("port"));
        Assert.assertNotNull(actual2.get("sSLInfo"));
        Assert.assertTrue(actual2.get("sSLInfo") instanceof Map);
        Map sslInfo = (Map) actual2.get("sSLInfo");
        Assert.assertEquals("false", sslInfo.get("clientAuthEnabled"));
        Assert.assertEquals("true", sslInfo.get("enabled"));
        Assert.assertEquals("false", sslInfo.get("ignoreValidationErrors"));
        Assert.assertEquals("key_alias", sslInfo.get("keyAlias"));
        Assert.assertEquals("keystore_name", sslInfo.get("keyStore"));
        Assert.assertEquals("truststore_name", sslInfo.get("trustStore"));
    }

    @Test(expected = IOException.class)
    public void testGetOrgConfigWithMissingFile() throws IOException {
        Path input = basePath.resolve("testGetOrgConfigWithMissingFile-input.json");
        ConfigReader.getOrgConfig(input.toFile());
    }

    @Test
    public void testGetOrgConfigApiProducts() throws IOException {
        Path input = basePath.resolve("testGetOrgConfigApiProducts-input.json");
        List actual = ConfigReader.getOrgConfig(input.toFile());
        Assert.assertNotNull(actual);
        Assert.assertEquals(2, actual.size());

        ObjectMapper om = new ObjectMapper();
        Map actual1 = om.readValue((String) actual.get(0), Map.class);
        Map expected1 = om.readValue(basePath.resolve("testGetOrgConfigApiProducts-expected.json").toFile(), Map.class);
        Assert.assertEquals(expected1, actual1);

        Map actual2 = om.readValue((String) actual.get(1), Map.class);
        Assert.assertEquals("myOtherProduct", actual2.get("name"));
        Assert.assertEquals("My other product", actual2.get("displayName"));
        Assert.assertEquals("This is my other awesome product", actual2.get("description"));
    }

    @Test(expected = IOException.class)
    public void testGetOrgConfigWithMissingYamlFile() throws IOException {
        Path input = basePath.resolve("testGetOrgConfigWithMissingYamlFile-input.yaml");
        ConfigReader.getOrgConfig(input.toFile());
    }

    @Test
    public void testGetOrgConfigApiProductsFromYaml() throws IOException {
        Path input = basePath.resolve("testGetOrgConfigApiProductsFromYaml-input.yaml");
        List actual = ConfigReader.getOrgConfig(input.toFile());
        Assert.assertNotNull(actual);
        Assert.assertEquals(2, actual.size());

        ObjectMapper om = new ObjectMapper();
        Map actual1 = om.readValue((String) actual.get(0), Map.class);
        Map expected1 = om.readValue(basePath.resolve("testGetOrgConfigApiProductsFromYaml-expected.json").toFile(), Map.class);
        Assert.assertEquals(expected1, actual1);

        Map actual2 = om.readValue((String) actual.get(1), Map.class);
        Assert.assertEquals("myOtherProduct", actual2.get("name"));
        Assert.assertEquals("My other product", actual2.get("displayName"));
        Assert.assertEquals("This is my other awesome product", actual2.get("description"));
    }

    @Test(expected = IOException.class)
    public void testGetAPIConfigWithMissingFile() throws IOException {
        Path input = basePath.resolve("testGetAPIConfigWithMissingFile-input.json");
        ConfigReader.getAPIConfig(input.toFile());
    }

    @Test
    public void testGetAPIConfigKVMs() throws IOException {
        Path input = basePath.resolve("testGetAPIConfigKVMs-input.json");
        List actual = ConfigReader.getAPIConfig(input.toFile());
        Assert.assertNotNull(actual);
        Assert.assertEquals(2, actual.size());

        ObjectMapper om = new ObjectMapper();
        Map actual1 = om.readValue((String) actual.get(0), Map.class);
        Map expected1 = om.readValue(basePath.resolve("testGetAPIConfigKVMs-expected.json").toFile(), Map.class);
        Assert.assertEquals(expected1, actual1);

        Map actual2 = om.readValue((String) actual.get(1), Map.class);
        Assert.assertEquals("my_other_config", actual2.get("name"));
        Assert.assertNotNull(actual2.get("entry"));
        Assert.assertTrue(actual2.get("entry") instanceof List);
        List entries = (List) actual2.get("entry");
        Assert.assertEquals(2, entries.size());
        Assert.assertTrue(entries.get(0) instanceof Map);
        Assert.assertEquals("MYNAME", ((Map) entries.get(0)).get("name"));
        Assert.assertEquals("MYVALUE", ((Map) entries.get(0)).get("value"));
        Assert.assertEquals("MYOTHERNAME", ((Map) entries.get(1)).get("name"));
        Assert.assertEquals("MYOTHERVALUE", ((Map) entries.get(1)).get("value"));
    }

    @Test(expected = IOException.class)
    public void testGetAPIConfigWithMissingYamlFile() throws IOException {
        Path input = basePath.resolve("testGetAPIConfigWithMissingYamlFile-input.yaml");
        ConfigReader.getAPIConfig(input.toFile());
    }

    @Test
    public void testGetAPIConfigKVMsFromYaml() throws IOException {
        Path input = basePath.resolve("testGetAPIConfigKVMsFromYaml-input.yaml");
        List actual = ConfigReader.getAPIConfig(input.toFile());
        Assert.assertNotNull(actual);
        Assert.assertEquals(2, actual.size());

        ObjectMapper om = new ObjectMapper();
        Map actual1 = om.readValue((String) actual.get(0), Map.class);
        Map expected1 = om.readValue(basePath.resolve("testGetAPIConfigKVMsFromYaml-expected.json").toFile(), Map.class);
        Assert.assertEquals(expected1, actual1);

        Map actual2 = om.readValue((String) actual.get(1), Map.class);
        Assert.assertEquals("my_other_config", actual2.get("name"));
        Assert.assertNotNull(actual2.get("entry"));
        Assert.assertTrue(actual2.get("entry") instanceof List);
        List entries = (List) actual2.get("entry");
        Assert.assertEquals(2, entries.size());
        Assert.assertTrue(entries.get(0) instanceof Map);
        Assert.assertEquals("MYNAME", ((Map) entries.get(0)).get("name"));
        Assert.assertEquals("MYVALUE", ((Map) entries.get(0)).get("value"));
        Assert.assertEquals("MYOTHERNAME", ((Map) entries.get(1)).get("name"));
        Assert.assertEquals("MYOTHERVALUE", ((Map) entries.get(1)).get("value"));
    }

    @Test
    public void testGetAPIList() {
        Set<String> actual = ConfigReader.getAPIList(basePath.resolve("testGetAPIList").toString());
        Assert.assertNotNull(actual);
        Assert.assertEquals(3, actual.size());
        Assert.assertTrue(actual.contains("my-api-v1"));
        Assert.assertTrue(actual.contains("my-api-v2"));
        Assert.assertTrue(actual.contains("my-awesome-api-v1"));
    }

    @Test(expected = IOException.class)
    public void testGetOrgConfigWithIdWithMissingFile() throws IOException {
        Path input = basePath.resolve("testGetOrgConfigWithIdWithMissingFile-input.json");
        ConfigReader.getOrgConfigWithId(input.toFile());
    }

    @Test
    public void testGetOrgConfigWithId() throws IOException {
        Path input = basePath.resolve("testGetOrgConfigWithId-input.json");
        Map actual = ConfigReader.getOrgConfigWithId(input.toFile());

        Assert.assertNotNull(actual);
        Assert.assertEquals(2, actual.size());
        Assert.assertTrue(actual.containsKey("grant@enterprise.com"));
        Assert.assertTrue(actual.containsKey("hugh@example.com"));

        // Developer 1
        ObjectMapper om = new ObjectMapper();
        Assert.assertNotNull(actual.get("grant@enterprise.com"));
        Assert.assertTrue(actual.get("grant@enterprise.com") instanceof List);
        List<Map> actual1 = new LinkedList<>();
        for (Object s : (List) actual.get("grant@enterprise.com")) {
            actual1.add(om.readValue((String) s, Map.class));
        }
        List expected1 = om.readValue(basePath.resolve("testGetOrgConfigWithId-expected1.json").toFile(), List.class);
        Assert.assertEquals(expected1, actual1);

        // Developer 2
        Assert.assertNotNull(actual.get("hugh@example.com"));
        Assert.assertTrue(actual.get("hugh@example.com") instanceof List);
        List<Map> actual2 = new LinkedList<>();
        for (Object s : (List) actual.get("hugh@example.com")) {
            actual2.add(om.readValue((String) s, Map.class));
        }
        List expected2 = om.readValue(basePath.resolve("testGetOrgConfigWithId-expected2.json").toFile(), List.class);
        Assert.assertEquals(expected2, actual2);
    }

    @Test(expected = IOException.class)
    public void testGetOrgConfigWithIdWithMissingYamlFile() throws IOException {
        Path input = basePath.resolve("testGetOrgConfigWithIdWithMissingYamlFile-input.json");
        ConfigReader.getOrgConfigWithId(input.toFile());
    }

    @Test
    public void testGetOrgConfigWithIdFromYaml() throws IOException {
        Path input = basePath.resolve("testGetOrgConfigWithIdFromYaml-input.yaml");
        Map actual = ConfigReader.getOrgConfigWithId(input.toFile());

        Assert.assertNotNull(actual);
        Assert.assertEquals(2, actual.size());
        Assert.assertTrue(actual.containsKey("grant@enterprise.com"));
        Assert.assertTrue(actual.containsKey("hugh@example.com"));

        // Developer 1
        ObjectMapper om = new ObjectMapper();
        Assert.assertNotNull(actual.get("grant@enterprise.com"));
        Assert.assertTrue(actual.get("grant@enterprise.com") instanceof List);
        List<Map> actual1 = new LinkedList<>();
        for (Object s : (List) actual.get("grant@enterprise.com")) {
            actual1.add(om.readValue((String) s, Map.class));
        }
        List expected1 = om.readValue(basePath.resolve("testGetOrgConfigWithIdFromYaml-expected1.json").toFile(), List.class);
        Assert.assertEquals(expected1, actual1);

        // Developer 2
        Assert.assertNotNull(actual.get("hugh@example.com"));
        Assert.assertTrue(actual.get("hugh@example.com") instanceof List);
        List<Map> actual2 = new LinkedList<>();
        for (Object s : (List) actual.get("hugh@example.com")) {
            actual2.add(om.readValue((String) s, Map.class));
        }
        List expected2 = om.readValue(basePath.resolve("testGetOrgConfigWithIdFromYaml-expected2.json").toFile(), List.class);
        Assert.assertEquals(expected2, actual2);
    }
}
