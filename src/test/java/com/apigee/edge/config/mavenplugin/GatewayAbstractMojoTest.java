package com.apigee.edge.config.mavenplugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;


class GatewayAbstractMojoTest {

    private GatewayAbstractMojo gatewayAbstractMojo;

    @BeforeEach
    void before() {
        gatewayAbstractMojo = new GatewayAbstractMojo() {

            @Override
            public void execute() throws MojoExecutionException, MojoFailureException {
                // Noop
            }
        };
        gatewayAbstractMojo.setConfigDir(Paths.get("src/test/resources", getClass().getName()).toString());
    }

    @Test
    void findConfigFileWithMissingFile() throws MojoExecutionException {
        File actual = gatewayAbstractMojo.findConfigFile("./", "findConfigFileWithMissingFile");
        Assert.assertNull(actual);
    }

    @Test
    void findConfigFile() throws MojoExecutionException {
        File actual = gatewayAbstractMojo.findConfigFile("./", "findConfigFile");
        Assert.assertNotNull(actual);
        Path expected = Paths.get("src/test/resources", getClass().getName(), "./findConfigFile.json");
        Assert.assertEquals(expected.toFile(), actual);
    }

    @Test
    void findConfigFileFromYaml() throws MojoExecutionException {
        File actual = gatewayAbstractMojo.findConfigFile("./", "findConfigFileFromYaml");
        Assert.assertNotNull(actual);
        Path expected = Paths.get("src/test/resources", getClass().getName(), "./findConfigFileFromYaml.yaml");
        Assert.assertEquals(expected.toFile(), actual);
    }

    @Test
    void findConsolidatedConfigFile() {
        Path baseDirectory = Paths.get("src/test/resources", getClass().getName(), "findConsolidatedConfigFile");
        gatewayAbstractMojo.setBaseDirectory(baseDirectory.toFile());

        File actual = gatewayAbstractMojo.findConsolidatedConfigFile();
        Assert.assertNotNull(actual);
        Path expected = Paths.get(baseDirectory.toString(), "edge.json").toAbsolutePath();
        Assert.assertEquals(expected.toFile(), actual);
    }
}
