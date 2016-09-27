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

import com.apigee.edge.config.utils.PrintUtil;
import com.apigee.edge.config.utils.ServerProfile;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.testing.http.MockHttpContent;
import com.google.api.client.util.GenericData;
import com.google.api.client.util.Key;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.net.URLEncoder;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.reflect.Type;

public class RestUtil {

    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();
    static String versionRevision;
    static Logger logger = LoggerFactory.getLogger(RestUtil.class);
    
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

    /***************************************************************************
     * Env Config - get, create, update
     **/
    public static HttpResponse createEnvConfig(ServerProfile profile, 
                                                String resource,
                                                String payload)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json", 
                                                            payload.getBytes());

        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/environments/"
                            + profile.getEnvironment() + "/" + resource;

        HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(
                new GenericUrl(importCmd), content);
        restRequest.setReadTimeout(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");
        headers.setBasicAuthentication(profile.getCredential_user(),
                profile.getCredential_pwd());
        restRequest.setHeaders(headers);

        logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
            response = restRequest.execute();
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public static HttpResponse updateEnvConfig(ServerProfile profile, 
                                                String resource,
                                                String resourceId,
                                                String payload)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json", 
                                                            payload.getBytes());

        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/environments/"
                            + profile.getEnvironment() + "/" + resource + "/"
                            + URLEncoder.encode(resourceId, "UTF-8");

        HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(
                new GenericUrl(importCmd), content);
        restRequest.setReadTimeout(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");
        headers.setBasicAuthentication(profile.getCredential_user(),
                profile.getCredential_pwd());
        restRequest.setHeaders(headers);

        logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
            response = restRequest.execute();
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public static HttpResponse deleteEnvConfig(ServerProfile profile, 
                                                String resource,
                                                String resourceId)
            throws IOException {

        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/environments/"
                            + profile.getEnvironment() + "/" + resource + "/"
                            + URLEncoder.encode(resourceId, "UTF-8");

        HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(
                new GenericUrl(importCmd));
        restRequest.setReadTimeout(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");
        headers.setBasicAuthentication(profile.getCredential_user(),
                profile.getCredential_pwd());
        restRequest.setHeaders(headers);

        logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
            response = restRequest.execute();
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public static HttpResponse getEnvConfig(ServerProfile profile, 
                                                String resource) 
            throws IOException {

        HttpRequest restRequest = REQUEST_FACTORY
                .buildGetRequest(new GenericUrl(profile.getHostUrl() + "/"
                        + profile.getApi_version() + "/organizations/"
                        + profile.getOrg() + "/environments/"
                        + profile.getEnvironment() + "/" + resource));
        restRequest.setReadTimeout(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");
        headers.setBasicAuthentication(profile.getCredential_user(),
                profile.getCredential_pwd());
        restRequest.setHeaders(headers);

        logger.debug(PrintUtil.formatRequest(restRequest));

        HttpResponse response = null;
        try {
            response = restRequest.execute();
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) return null;
            logger.error(e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    /***************************************************************************
     * Org Config - get, create, update
     **/
    public static HttpResponse createOrgConfig(ServerProfile profile, 
                                                String resource,
                                                String payload)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json", 
                                                            payload.getBytes());

        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/" + resource;

        HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(
                new GenericUrl(importCmd), content);
        restRequest.setReadTimeout(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");
        headers.setBasicAuthentication(profile.getCredential_user(),
                profile.getCredential_pwd());
        restRequest.setHeaders(headers);

        logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
            response = restRequest.execute();
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public static HttpResponse updateOrgConfig(ServerProfile profile, 
                                                String resource,
                                                String resourceId,
                                                String payload)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json", 
                                                            payload.getBytes());

        String importCmd = profile.getHostUrl() + "/"
                                + profile.getApi_version() + "/organizations/"
                                + profile.getOrg() + "/" + resource + "/"
                                + URLEncoder.encode(resourceId, "UTF-8");

        HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(
                new GenericUrl(importCmd), content);
        restRequest.setReadTimeout(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");
        headers.setBasicAuthentication(profile.getCredential_user(),
                profile.getCredential_pwd());
        restRequest.setHeaders(headers);

        logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
            response = restRequest.execute();
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public static HttpResponse deleteOrgConfig(ServerProfile profile, 
                                                String resource,
                                                String resourceId)
            throws IOException {

        String importCmd = profile.getHostUrl() + "/"
                                + profile.getApi_version() + "/organizations/"
                                + profile.getOrg() + "/" + resource + "/"
                                + URLEncoder.encode(resourceId, "UTF-8");

        HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(
                                                    new GenericUrl(importCmd));
        restRequest.setReadTimeout(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");
        headers.setBasicAuthentication(profile.getCredential_user(),
                profile.getCredential_pwd());
        restRequest.setHeaders(headers);

        logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
            response = restRequest.execute();
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public static HttpResponse getOrgConfig(ServerProfile profile, 
                                                String resource) 
            throws IOException {

        HttpRequest restRequest = REQUEST_FACTORY.buildGetRequest(
                new GenericUrl(profile.getHostUrl() + "/"
                        + profile.getApi_version() + "/organizations/"
                        + profile.getOrg() + "/" + resource));
        restRequest.setReadTimeout(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");
        headers.setBasicAuthentication(profile.getCredential_user(),
                profile.getCredential_pwd());
        restRequest.setHeaders(headers);

        logger.debug(PrintUtil.formatRequest(restRequest));

        HttpResponse response = null;
        try {
            response = restRequest.execute();
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) return null;
            logger.error(e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    /***************************************************************************
     * API Config - get, create, update
     **/
        public static HttpResponse createAPIConfig(ServerProfile profile, 
                                                    String api,
                                                    String resource,
                                                    String payload)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json", 
                                                            payload.getBytes());

        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/apis/"
                            + api + "/" + resource;

        HttpRequest restRequest = REQUEST_FACTORY.buildPostRequest(
                new GenericUrl(importCmd), content);
        restRequest.setReadTimeout(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");
        headers.setBasicAuthentication(profile.getCredential_user(),
                profile.getCredential_pwd());
        restRequest.setHeaders(headers);

        logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
            response = restRequest.execute();
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public static HttpResponse updateAPIConfig(ServerProfile profile, 
                                                String api,
                                                String resource,
                                                String resourceId,
                                                String payload)
            throws IOException {

        ByteArrayContent content = new ByteArrayContent("application/json", 
                                                            payload.getBytes());

        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/apis/"
                            + api + "/" + resource + "/"
                            + URLEncoder.encode(resourceId, "UTF-8");

        HttpRequest restRequest = REQUEST_FACTORY.buildPutRequest(
                new GenericUrl(importCmd), content);
        restRequest.setReadTimeout(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");
        headers.setBasicAuthentication(profile.getCredential_user(),
                profile.getCredential_pwd());
        restRequest.setHeaders(headers);

        logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
            response = restRequest.execute();
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public static HttpResponse deleteAPIConfig(ServerProfile profile, 
                                                String api,
                                                String resource,
                                                String resourceId)
            throws IOException {


        String importCmd = profile.getHostUrl() + "/"
                            + profile.getApi_version() + "/organizations/"
                            + profile.getOrg() + "/apis/"
                            + api + "/" + resource + "/"
                            + URLEncoder.encode(resourceId, "UTF-8");

        HttpRequest restRequest = REQUEST_FACTORY.buildDeleteRequest(
                new GenericUrl(importCmd));
        restRequest.setReadTimeout(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");
        headers.setBasicAuthentication(profile.getCredential_user(),
                profile.getCredential_pwd());
        restRequest.setHeaders(headers);

        logger.info(PrintUtil.formatRequest(restRequest));

        HttpResponse response;
        try {
            response = restRequest.execute();
        } catch (HttpResponseException e) {
            logger.error("Apigee call failed " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }

    public static HttpResponse getAPIConfig(ServerProfile profile,
                                                String api,
                                                String resource) 
            throws IOException {

        HttpRequest restRequest = REQUEST_FACTORY
                .buildGetRequest(new GenericUrl(profile.getHostUrl() + "/"
                        + profile.getApi_version() + "/organizations/"
                        + profile.getOrg() + "/apis/"
                        + api + "/" + resource));
        restRequest.setReadTimeout(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");
        headers.setBasicAuthentication(profile.getCredential_user(),
                profile.getCredential_pwd());
        restRequest.setHeaders(headers);

        logger.debug(PrintUtil.formatRequest(restRequest));

        HttpResponse response = null;
        try {
            response = restRequest.execute();
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) return null;
            logger.error(e.getMessage());
            throw new IOException(e.getMessage());
        }

        return response;
    }


}
