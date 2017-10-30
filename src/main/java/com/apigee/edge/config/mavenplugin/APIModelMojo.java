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

import com.apigee.edge.config.rest.PortalRestUtil;
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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.api.client.http.*;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**                                                                                                                                     ¡¡
 * Goal to create API Models in Apigee Developer Portal
 * scope: org
 *
 * @author william.oconnor
 * @goal apimodel
 * @phase install
 */

public class APIModelMojo extends GatewayAbstractMojo {
  static Logger logger = LoggerFactory.getLogger(APIModelMojo.class);
  private static File[] files = null;

  public static final String ____ATTENTION_MARKER____ =
  "************************************************************************";

  enum OPTIONS {
    none, create, update, delete, render
  }

  OPTIONS buildOption = OPTIONS.none;

  private ServerProfile serverProfile;

  public static class APIModel {
    @Key
    public String title;
  }

  /**
   * Constructor.
   */
  public APIModelMojo() {
    super();
  }

  public void init() throws MojoExecutionException, MojoFailureException {
    try {
      logger.info(____ATTENTION_MARKER____);
      logger.info("Smart Docs");
      logger.info(____ATTENTION_MARKER____);

      String options="";
      serverProfile = super.getProfile();

      options = super.getOptions();
      if (options != null) {
        buildOption = OPTIONS.valueOf(options);
      }
      if (buildOption == OPTIONS.none) {
        logger.info("Skipping APIModel (default action)");
        return;
      }

      logger.debug("Build option " + buildOption.name());
      logger.debug("Portal Path " + serverProfile.getPortalPath());

      // Ensure we have parameters (type, directory, url, path, credentials)
      if (serverProfile.getPortalFormat() == null) {
        throw new MojoExecutionException(
          "Developer portal file format not found in profile");
      }
      if (serverProfile.getPortalURL() == null) {
        throw new MojoExecutionException(
          "Developer portal URL not found in profile");
      }
      if (serverProfile.getPortalPath() == null) {
        throw new MojoExecutionException(
          "Developer portal path not found in profile");
      }
      if (serverProfile.getPortalUserName() == null) {
        throw new MojoExecutionException(
          "Developer portal username not found in profile");
      }
      if (serverProfile.getPortalPassword() == null) {
        throw new MojoExecutionException(
          "Developer portal password not found in profile");
      }

      // Scan to make sure there are swagger files to send.
      getOpenAPISpecs();

    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Invalid apigee.option provided");
    } catch (RuntimeException e) {
      throw e;
    } catch (MojoExecutionException e) {
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

    Logger logger = LoggerFactory.getLogger(APIModelMojo.class);

    try {
      init();
      if (buildOption == OPTIONS.none) {
        return;
      }
      
      if (buildOption == OPTIONS.create ||
        buildOption == OPTIONS.update) {
        doUpdate();
      }
      
      if (buildOption == OPTIONS.delete) {
        doDelete();
      }
      
      if (buildOption == OPTIONS.render) {
        doRender();
      }

    } catch (MojoFailureException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    }
  }

  /**
   * Posts an update based on available OpenAPI specs. Will update base
   * information, then upload the current OpenAPI spec.
   */
  public void doUpdate() throws MojoExecutionException {
    try {
      for (File file : files) {
        PortalRestUtil.postAPIModel(serverProfile, file);
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Update failure: " + e.getMessage());
    }
  }
  
  /**
   * Sends a render request for models that exist in the file system.
   */
  public void doRender() throws MojoExecutionException {
    try {
      for (File file : files) {
        PortalRestUtil.renderAPIModel(serverProfile, file);
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Render failure: " + e.getMessage());
    }
  }
  
  /**
   * Deletes models that exist in the API and not in the file system.
   */
  public void doDelete() throws MojoExecutionException {
    try {
      for (File file : files) {
        PortalRestUtil.deleteAPIModel(serverProfile, "Fake");
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Deletion failure: " + e.getMessage());
    }
  }

  /**
   * Pulls a list of OpenAPI specs frpm a directory to be sent 
   * to a Developer Portal instance.
   */
  public void getOpenAPISpecs() throws MojoExecutionException {
    // Ensure we have a directory to read.
    if (serverProfile.getPortalDirectory() == null) {
      throw new MojoExecutionException(
        "Developer portal directory not found in profile");
    }

    // Scan the directory for files.
    String directory = serverProfile.getPortalDirectory();
    logger.info("Get OpenAPI Specs from " + directory);
    files = new File(directory).listFiles();
  }

}




