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
package com.apigee.edge.config.rest;

import java.io.File;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apigee.edge.config.utils.PrintUtil;
import com.apigee.edge.config.utils.ServerProfile;
import com.apigee.mgmtapi.sdk.client.MgmtAPIClient;
import com.apigee.mgmtapi.sdk.model.AccessToken;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

import com.google.api.client.util.Key;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.FileNotFoundException;

public class PortalRestUtil {

    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();
    static String versionRevision;
    static Logger logger = LoggerFactory.getLogger(RestUtil.class);
    static String accessToken = null;

    static HttpRequestFactory REQUEST_FACTORY = HTTP_TRANSPORT
      .createRequestFactory(new HttpRequestInitializer() {
        // @Override
        public void initialize(HttpRequest request) {
          request.setParser(JSON_FACTORY.createJsonObjectParser());
          XTrustProvider.install();
          FakeHostnameVerifier _hostnameVerifier = new FakeHostnameVerifier();
          // Install the all-trusting host name verifier:
          HttpsURLConnection.setDefaultHostnameVerifier(_hostnameVerifier);
        }
      });

    // Header values to ensure headers are set and then stored.
    public static Boolean headersSet = false;
    public static HttpHeaders headers = null;

    // Class to handle our authentication response.
    public static class AuthObject {
      @Key
      public String token;
      public String sessid;
      public String session_name;
    }
    
    public static class SpecObject {
      public SpecInfoObject info;
      
      public String getName() {
        return info.getTitle().replace(" ", "-");
      }
      
      public String getTitle() {
        return info.getTitle();
      }
      
      public String getDescription() {
        return info.getDescription();
      }
    }
    
    public static class SpecInfoObject {
      public String title;
      public String description;
      public String version;
      
      public String getTitle() {
        return title;
      }
      public String getDescription() {
        return description;   
      }
    }
    
    public static class ModelObject {
      public String id;
      public String name;
      public String displayName;
      public String description;
      public String latestRevisionNumber;
      public String createdTime;
      public String modifiedTime;
    }

    // Get headers that have been set.
    public static HttpHeaders getHeaders() {
      return headers;
    }

    // Store/update headers.
    public static HttpHeaders setHeaders(HttpHeaders tempHeaders) {
      headers = tempHeaders;
      return headers;
    }

  /**
   * Authenticate against the Developer portal and set necessary headers
   * to facilitate additional transactions.
   */
  public static HttpResponse authenticate(ServerProfile profile) throws IOException {
    HttpResponse response = null;
    if (headersSet == false) {
      String payload = "{\"username\": \"" + profile.getPortalUserName() +
        "\", \"password\":\"" + profile.getPortalPassword() + "\"}";
      ByteArrayContent content = new ByteArrayContent("application/json",
        payload.getBytes());

      HttpRequest restRequest = REQUEST_FACTORY
        .buildPostRequest(new GenericUrl(
          profile.getPortalURL() + "/" + profile.getPortalPath() +
          "/user/login.json"), content);
      restRequest.setReadTimeout(0);

      try {
        response = restRequest.execute();

        InputStream source = response.getContent(); //Get the data in the entity
        Reader reader = new InputStreamReader(source);

        Gson gson = new Gson();
        AuthObject auth = gson.fromJson(reader, AuthObject.class);
        headersSet = true;

        HttpHeaders tempHeaders = new HttpHeaders();
        tempHeaders.setCookie(auth.session_name + "=" + auth.sessid);
        tempHeaders.set("X-CSRF-Token", auth.token);
        setHeaders(tempHeaders);

      }
      catch (HttpResponseException e) {
        logger.error(e.getMessage());
        // Throw an error as there is no point in continuing.
        throw e;
      }
    }

    return response;
  }
  
  /**
   * Retrieve an existing model element. Returns null if the model does
   * not currently exist.
   */
  public static ModelObject getAPIModel(ServerProfile profile, File file) throws IOException {
    HttpResponse response = null;
    try {
      // First authenticate.
      authenticate(profile);
      
      SpecObject spec = parseSpec(file);
      HttpRequest restRequest = REQUEST_FACTORY
        .buildGetRequest(new GenericUrl(
          profile.getPortalURL() + "/" + profile.getPortalPath() +
          "/smartdocs/" + spec.getName() + ".json"));
          restRequest.setReadTimeout(0);
      logger.info("Retrieve " + spec.getTitle() + " model.");
      
      response = restRequest.execute();
      
      Gson gson = new Gson();
      InputStream source = response.getContent(); //Get the data in the entity
      Reader reader = new InputStreamReader(source);
      ModelObject model = gson.fromJson(reader, ModelObject.class);
      return model;
    }
    catch (HttpResponseException e) {
      if (e.getStatusCode() == 404) {
        logger.info("Model does not currently exist.");
        return null;
      }
      else {
        throw e;
      }
    }
  }
  
  /**
   * Create a base model. The model must exist prior to sending 
   * an OpenAPI spec or the OpenAPI spec send will fail.
   */
  public static void createAPIModel(ServerProfile profile, File file) throws IOException {
    HttpResponse response = null;
    try {
      SpecObject spec = parseSpec(file);
      ByteArrayContent content = getAPIModelContent(profile, spec);
      HttpRequest restRequest = REQUEST_FACTORY
        .buildPostRequest(new GenericUrl(
          profile.getPortalURL() + "/" + profile.getPortalPath() +
          "/smartdocs.json"), content);
      restRequest.setReadTimeout(0);
      logger.info("Creating " + spec.getTitle() + " model.");
      
      response = restRequest.execute();
      logger.info(response.parseAsString());
    }
    catch (HttpResponseException e) {
      throw e;
    }
  }
  
  /**
   * Update an existing model. Resending a create command will
   * force a new revision of the model. This simply updates parameters of 
   * a model without modifying the version information.
   */
  public static void updateAPIModel(ServerProfile profile, File file) throws IOException {
    HttpResponse response = null;
    try {
      SpecObject spec = parseSpec(file);
      ByteArrayContent content = getAPIModelContent(profile, spec);
      HttpRequest restRequest = REQUEST_FACTORY
        .buildPutRequest(new GenericUrl(
          profile.getPortalURL() + "/" + profile.getPortalPath() +
          "/smartdocs/" + spec.getName() + ".json"), content);
      restRequest.setReadTimeout(0);
      logger.info("Updating " + spec.getTitle() + " model.");
      
      response = restRequest.execute();
      logger.info(response.parseAsString());
    }
    catch (HttpResponseException e) {
      throw e;
    }
  }
  
  /**
   * Delete an existing model. 
   */
  public static void deleteAPIModel(ServerProfile profile, String specName) throws IOException {
    HttpResponse response = null;
    try {
      HttpRequest restRequest = REQUEST_FACTORY
        .buildDeleteRequest(new GenericUrl(
          profile.getPortalURL() + "/" + profile.getPortalPath() +
          "/smartdocs/" + specName + ".json"));
      restRequest.setReadTimeout(0);
      logger.info("Deleting " + specName + " model.");
      
      response = restRequest.execute();
      logger.info(response.parseAsString());
    }
    catch (HttpResponseException e) {
      throw e;
    }
  }
  
  /**
   * Render an existing model. 
   */
  public static void renderAPIModel(ServerProfile profile, File file) throws IOException {
    HttpResponse response = null;
    try {
      SpecObject spec = parseSpec(file);
      ByteArrayContent content = new ByteArrayContent("application/json", 
        "".getBytes());
      HttpRequest restRequest = REQUEST_FACTORY
        .buildPostRequest(new GenericUrl(
          profile.getPortalURL() + "/" + profile.getPortalPath() +
          "/smartdocs/" + spec.getName() + "/render.json"), content);
      restRequest.setReadTimeout(0);
      logger.info("Rendering " + spec.getTitle() + " OpenAPI spec.");
      
      response = restRequest.execute();
      logger.info(response.parseAsString());
    }
    catch (HttpResponseException e) {
      throw e;
    }
  }
  
  /**
   * Helper function to build the body for model creations and updates.
   */
  private static ByteArrayContent getAPIModelContent(ServerProfile profile, 
    SpecObject spec) 
    throws IOException {

    try {
      // First authenticate.
      authenticate(profile);
      
      String description = "";
      if (spec.getDescription() != null) {
        description = spec.getDescription();
      }
      
      String payload = "{" + 
        "\"name\": \"" + spec.getName() + "\"," +
        "\"display_name\":\"" + spec.getTitle()+ "\"," +
        "\"description\":\"" + description + "\"" +
      "}";
      ByteArrayContent content = new ByteArrayContent("application/json",
        payload.getBytes());
      
      return content;
    }
    catch (HttpResponseException e) {
      throw e;
    }
  }

  /**
   * Posts the OpenAPI Spec to a APIModel in Developer Portal.
   */
  public static void postAPIModel(ServerProfile profile, File file) throws IOException {
    HttpResponse response = null;
    try {
      // First authenticate.
      authenticate(profile);
      
      if (getAPIModel(profile, file) == null) {
        createAPIModel(profile, file);
      }
      else {
        updateAPIModel(profile, file);
      }

      FileContent tempFileContent = new FileContent("application/json", file);
      SpecObject spec = parseSpec(file);
        
      // Then build the OpenAPI Spec command.
      MultipartContent.Part filePart = new MultipartContent.Part(tempFileContent)
        .setHeaders(new HttpHeaders().set(
          "Content-Disposition", 
          String.format("form-data; name=\"api_definition\"; filename=\"%s\"", file.getName())
        ));
    
      MultipartContent.Part typePart = new MultipartContent.Part(
          new ByteArrayContent(null, "json".getBytes())
        )
        .setHeaders(new HttpHeaders().set(
          "Content-Disposition", 
          "form-data; name=\"type\""
        ));
    
      MultipartContent.Part namePart = new MultipartContent.Part(
          new ByteArrayContent(null, spec.getTitle().getBytes())
        )
        .setHeaders(new HttpHeaders().set(
          "Content-Disposition", 
          "form-data; name=\"name\""
        ));
    
      MultipartContent content = new MultipartContent().setMediaType(
        new HttpMediaType("multipart/form-data")
          .setParameter("boundary", "__END_OF_PART__")
      );
      content.addPart(filePart);
      content.addPart(typePart);
      content.addPart(namePart);

      HttpRequest restRequest = REQUEST_FACTORY
        .buildPostRequest(new GenericUrl(
          profile.getPortalURL() + "/" + profile.getPortalPath() +
          "/smartdocs/" + spec.getName() + "/import.json"), content);
      restRequest.setReadTimeout(0);
      logger.info("Posting " + spec.getTitle() + " OpenAPI spec.");
      
      response = restRequest.execute();
      logger.info(response.parseAsString());

    // Finally push the OpenAPI Spec and communicate response.
    }
    catch (HttpResponseException e) {
      logger.error(e.getMessage());
      throw e;
    }
  }
  
  /**
   * Helper function to parse a json formatted OpenAPI spec and turn it into
   * an object containing the properties we will reuse. 
   */
  private static SpecObject parseSpec(File file) throws FileNotFoundException {
    FileContent tempFileContent = new FileContent("application/json", file);
    Reader reader = new InputStreamReader(tempFileContent.getInputStream());

    Gson gson = new Gson();
    SpecObject spec = gson.fromJson(reader, SpecObject.class);

    return spec;
  }

}
