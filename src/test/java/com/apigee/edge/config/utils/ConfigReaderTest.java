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

public class ConfigReaderTest {

    private final Path basePath = Paths.get("src/test/resources", getClass().getName());

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
        Path input = basePath.resolve("testGetOrgConfigWithMissingFile-input.yaml");
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
}
