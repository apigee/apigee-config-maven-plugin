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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apigee.edge.config.rest.RestUtil;
import com.apigee.edge.config.utils.ServerProfile;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.util.Key;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;


/**                                                                                                                                     ¡¡
 * Goal to create Custom Report in Apigee
 * scope: org
 *
 * @author ssvaidyanathan
 * @goal reports
 * @phase install
 */
public class ReportsMojo extends GatewayAbstractMojo {
	static Logger logger = LoggerFactory.getLogger(ReportsMojo.class);
	public static final String ____ATTENTION_MARKER____ = "************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

	public static class Report {
		@Key
		public String displayName;
	}

	public ReportsMojo() {
		super();

	}

	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Custom Report");
			logger.info(____ATTENTION_MARKER____);

			String options = "";
			serverProfile = super.getProfile();

			options = super.getOptions();
			if (options != null) {
				buildOption = OPTIONS.valueOf(options);
			}
			logger.debug("Build option " + buildOption.name());
			logger.debug("Base dir " + super.getBaseDirectoryPath());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Invalid apigee.option provided");
		} catch (RuntimeException e) {
			throw e;
		}

	}

	protected String getReportDisplayName(String payload) throws MojoFailureException {
		Gson gson = new Gson();
		try {
			Report report = gson.fromJson(payload, Report.class);
			return report.displayName;
		} catch (JsonParseException e) {
			throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(List<String> reports) throws MojoFailureException {
		try {
			Map<String, String> existingReportsMap = null;
			if (buildOption != OPTIONS.update && buildOption != OPTIONS.create && buildOption != OPTIONS.delete
					&& buildOption != OPTIONS.sync) {
				return;
			}

			logger.info("Retrieving existing Custom Reports");
			existingReportsMap = getReportMap(serverProfile);

			for (String report : reports) {
				String reportDisplayName = getReportDisplayName(report);
				if (reportDisplayName == null) {
					throw new IllegalArgumentException("Custom Report does not have a displayName.\n" + report + "\n");
				}

				if (existingReportsMap.containsValue(reportDisplayName)) {
					switch (buildOption) {
					case update:
						logger.info("Custom Report \"" + reportDisplayName + "\" exists. Updating.");
						updateReport(serverProfile, getKeyByValue(existingReportsMap, reportDisplayName), report);
						break;
					case create:
						logger.info("Custom Report \"" + reportDisplayName + "\" already exists. Skipping.");
						break;
					case delete:
						logger.info("Custom Report \"" + reportDisplayName + "\" already exists. Deleting.");
						deleteReport(serverProfile, getKeyByValue(existingReportsMap, reportDisplayName));
						break;
					case sync:
						logger.info(
								"Custom Report \"" + reportDisplayName + "\" already exists. Deleting and recreating.");
						deleteReport(serverProfile, getKeyByValue(existingReportsMap, reportDisplayName));
						logger.info("Creating Custom Report - " + reportDisplayName);
						createReport(serverProfile, report);
						break;
					}
				} else {
					switch (buildOption) {
					case create:
					case sync:
					case update:
						logger.info("Creating Custom Report - " + reportDisplayName);
						createReport(serverProfile, report);
						break;
					case delete:
						logger.info("Custom Report \"" + reportDisplayName + "\" does not exist. Skipping.");
						break;
					}
				}
			}

		} catch (IOException e) {
			throw new MojoFailureException("Apigee network call error " + e.getMessage());
		} catch (RuntimeException e) {
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

		Logger logger = LoggerFactory.getLogger(ReportsMojo.class);

		try {

			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Custom Report (default action)");
				return;
			}

			if (serverProfile.getEnvironment() == null) {
				throw new MojoExecutionException("Apigee environment not found in profile");
			}

			List reports = getOrgConfig(logger, "reports");
			if (reports == null || reports.size() == 0) {
				logger.info("No Custom Reports found.");
				return;
			}

			doUpdate(reports);

		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

	/***************************************************************************
	 * REST call wrappers
	 **/
	public static String createReport(ServerProfile profile, String report) throws IOException {
		RestUtil restUtil = new RestUtil(profile);
		HttpResponse response = restUtil.createOrgConfig(profile, "reports", report);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Create Success.");

		} catch (HttpResponseException e) {
			logger.error("Custom Report create error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}

	public static String updateReport(ServerProfile profile, String reportName, String report) throws IOException {
		RestUtil restUtil = new RestUtil(profile);
		HttpResponse response = restUtil.updateOrgConfig(profile, "reports", reportName, report);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Update Success.");

		} catch (HttpResponseException e) {
			logger.error("Custom report update error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}

	public static String deleteReport(ServerProfile profile, String reportName) throws IOException {
		RestUtil restUtil = new RestUtil(profile);
		HttpResponse response = restUtil.deleteOrgConfig(profile, "reports", reportName);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Delete Success.");

		} catch (HttpResponseException e) {
			logger.error("Custom Report delete error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}

	public static Map<String, String> getReportMap(ServerProfile profile) throws IOException {
		RestUtil restUtil = new RestUtil(profile);
		HttpResponse response = restUtil.getOrgConfig(profile, "reports");
		if (response == null)
			return new HashMap<String, String>();
		Map<String, String> reportNames = new HashMap<String, String>();
		try {
			logger.debug("output " + response.getContentType());
			// response can be read only once
			String payload = response.parseAsString();
			logger.debug(payload);

			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject) parser.parse(payload);
			JSONArray reports = (JSONArray) obj.get("qualifier");
			if (reports != null && reports.size() > 0) {
				for (Object report : reports) {
					reportNames.put(((JSONObject) report).get("name").toString(),
							((JSONObject) report).get("displayName").toString());
				}
			}
		} catch (ParseException pe) {
			logger.error("Get Custom Report parse error " + pe.getMessage());
			throw new IOException(pe.getMessage());
		} catch (HttpResponseException e) {
			logger.error("Get Custom Report error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return reportNames;
	}

	public static String getKeyByValue(Map<String, String> map, String value) {
		String key = null;
		for (Entry<String, String> entry : map.entrySet()) {
			if (entry.getValue().equals(value)) {
				key = entry.getKey();
			}
		}
		return key;
	}
}
