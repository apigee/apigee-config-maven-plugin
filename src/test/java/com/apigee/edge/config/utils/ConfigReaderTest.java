package com.apigee.edge.config.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigReaderTest {

    private final Path basePath = Paths.get("src/test/resources", getClass().getName());

    @Test(expected = IOException.class)
    public void testGetEnvConfigWithMissingFile() throws IOException, ParseException {
        Path input = basePath.resolve("testGetEnvConfigWithMissingFile-input.json");
        ConfigReader.getOrgConfig(input.toFile());
    }

    @Test
    public void testGetEnvConfigTargetServers() throws IOException, ParseException {
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
    public void testGetEnvConfigWithMissingYamlFile() throws IOException, ParseException {
        Path input = basePath.resolve("testGetEnvConfigWithMissingYamlFile-input.yaml");
        ConfigReader.getOrgConfig(input.toFile());
    }

    @Test
    public void testGetEnvConfigTargetServersFromYaml() throws IOException, ParseException {
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
    public void testGetOrgConfigWithMissingFile() throws IOException, ParseException {
        Path input = basePath.resolve("testGetOrgConfigWithMissingFile-input.json");
        ConfigReader.getOrgConfig(input.toFile());
    }

    @Test
    public void testGetOrgConfigApiProducts() throws IOException, ParseException {
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
    public void testGetOrgConfigWithMissingYamlFile() throws IOException, ParseException {
        Path input = basePath.resolve("testGetOrgConfigWithMissingYamlFile-input.yaml");
        ConfigReader.getOrgConfig(input.toFile());
    }

    @Test
    public void testGetOrgConfigApiProductsFromYaml() throws IOException, ParseException {
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
    public void testGetAPIConfigWithMissingFile() throws IOException, ParseException {
        Path input = basePath.resolve("testGetAPIConfigWithMissingFile-input.json");
        ConfigReader.getAPIConfig(input.toFile());
    }

    @Test
    public void testGetAPIConfigKVMs() throws IOException, ParseException {
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
    public void testGetAPIConfigWithMissingYamlFile() throws IOException, ParseException {
        Path input = basePath.resolve("testGetAPIConfigWithMissingYamlFile-input.yaml");
        ConfigReader.getAPIConfig(input.toFile());
    }

    @Test
    public void testGetAPIConfigKVMsFromYaml() throws IOException, ParseException {
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
}
