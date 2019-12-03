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
import java.util.ArrayList;
import java.util.List;

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

/**
 * Goal to create Custom Roles in Apigee EDGE. scope: org
 *
 * @author Abhishek Chouksey,
 * @author Pallavi Tanpure,
 * @author Soudnya Nalawade,
 * @author Himanshu Sachdev
 * @goal customroles
 * @phase install
 */

public class CustomRoleMojo extends GatewayAbstractMojo {
	public static final String userRole = "userroles";
	static Logger logger = LoggerFactory.getLogger(CustomRoleMojo.class);
	public static final String ____ATTENTION_MARKER____ = "************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

	public static class CustomRole {
		@Key
		public String name;
		public List<ResourcePermission> resourcepermissions;
	}

	public class ResourcePermission {

		String path;
		String[] permissions;
	}

	public CustomRoleMojo() {
		super();

	}

	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee Custom Roles");
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

	protected String getCustomRoleName(String payload) throws MojoFailureException {
		Gson gson = new Gson();
		try {
			CustomRole customrole = gson.fromJson(payload, CustomRole.class);
			return customrole.name;

		} catch (JsonParseException e) {
			throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(List<String> customRoles) throws MojoFailureException {
		try {
			List existingcustomRoles = null;
			if (buildOption != OPTIONS.update && buildOption != OPTIONS.create && buildOption != OPTIONS.delete
					&& buildOption != OPTIONS.sync) {
				return;
			}

			logger.info("Retrieving existing environment Custom Roles - ");
			existingcustomRoles = getCustomRoles(serverProfile);

			for (String customRole : customRoles) {
				String customRoleName = getCustomRoleName(customRole);
				if (customRoleName == null) {
					throw new IllegalArgumentException("Custom Role does not have a name.\n" + customRole + "\n");
				}

				if (existingcustomRoles.contains(customRoleName)) {
					switch (buildOption) {
					case update:
						logger.info("Custom Role \"" + customRoleName + "\" exists. Updating.");
						updateCustomRole(serverProfile, customRole);
						break;
					case create:
						logger.info("Custom Role \"" + customRoleName + "\" already exists. Skipping.");
						break;
					case delete:
						logger.info("Custom Role \"" + customRoleName + "\" already exists. Deleting.");
						deleteCustomRole(serverProfile, customRoleName);
						break;
					case sync:
						logger.info("Custom Role \"" + customRoleName + "\" already exists. Deleting and recreating.");
						deleteCustomRole(serverProfile, customRoleName);
						logger.info("Creating Custom Role - " + customRoleName);
						createCustomRole(serverProfile, customRole);
						break;
					}
				} else {
					switch (buildOption) {
					case create:
					case sync:
					case update:
						logger.info("Creating Custom Role - " + customRoleName);
						createCustomRole(serverProfile, customRole);
						break;
					case delete:
						logger.info("Custom Role \"" + customRoleName + "\" does not exist. Skipping.");
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

		Logger logger = LoggerFactory.getLogger(CustomRoleMojo.class);

		try {

			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping Custom Roles (default action)");
				return;
			}

			if (serverProfile.getEnvironment() == null) {
				throw new MojoExecutionException("Apigee environment not found in profile");
			}
			List customRoles = getOrgConfig(logger, userRole);
			if (customRoles == null || customRoles.size() == 0) {
				logger.info("No Custom Role config found.");
				return;
			}

			doUpdate(customRoles);

		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

	/***************************************************************************
	 * REST call wrappers
	 **/
	public static String createCustomRole(ServerProfile profile, String customRole) throws IOException {

		Gson gson = new Gson();
		CustomRole customRoleObject = gson.fromJson(customRole, CustomRole.class);
		String namePayload = customRoleConversion(customRoleObject.name);

		HttpResponse response = RestUtil.createOrgConfig(profile, userRole, namePayload);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode()) {
				logger.info("Create Success for role.");
				addResourcePermissionsToRole(profile, customRoleObject);
			}

		} catch (HttpResponseException e) {
			logger.error("Custom Role create error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}

	/**
	 * This method Assign Multiple Resource Permissions to the user role .
	 * 
	 * @param profile
	 * 
	 * @param customRoleObject user role Object
	 */
	private static void addResourcePermissionsToRole(ServerProfile profile, CustomRole customRoleObject)
			throws IOException {

		String permissionsPayload = customRolePermissionConversion(customRoleObject.resourcepermissions);
		HttpResponse response = RestUtil.createOrgConfig(profile,
				userRole + "/" + customRoleObject.name + "/resourcepermissions", permissionsPayload);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode()) {
				logger.info("Create Success for role permissions.");
			}

		} catch (HttpResponseException e) {
			logger.error("Custom Role create error " + e.getMessage());
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * This method is used to update the user role Permissions.
	 *
	 * @param profile
	 *
	 * @param customRoleName user role name
	 *
	 * @param customRole
	 */
	public static String updateCustomRole(ServerProfile profile, String customRole) throws IOException {
		Gson gson = new Gson();
		CustomRole customRoleObject = gson.fromJson(customRole, CustomRole.class);
		addResourcePermissionsToRole(profile, customRoleObject);
		return "";
	}

	public static String deleteCustomRole(ServerProfile profile, String customRoleName) throws IOException {

		HttpResponse response = RestUtil.deleteOrgConfig(profile, userRole, customRoleName);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Delete Success.");

		} catch (HttpResponseException e) {
			logger.error("Custom Role delete error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}

	public static List getCustomRoles(ServerProfile profile) throws IOException {

		HttpResponse response = RestUtil.getOrgConfig(profile, userRole);
		if (response == null)
			return new ArrayList();
		JSONArray customRoles = null;
		try {
			logger.debug("output " + response.getContentType());
			/**
			 * response can be read only once
			 */
			String payload = response.parseAsString();
			logger.debug(payload);

			/*
			 * Parsers fail to parse a string array. converting it to an JSON object as a
			 * workaround
			 */
			String obj = "{ \"customRoles\": " + payload + "}";

			JSONParser parser = new JSONParser();
			JSONObject obj1 = (JSONObject) parser.parse(obj);
			customRoles = (JSONArray) obj1.get("customRoles");

		} catch (ParseException pe) {
			logger.error("Get Custom Role parse error " + pe.getMessage());
			throw new IOException(pe.getMessage());
		} catch (HttpResponseException e) {
			logger.error("Get Custom Role error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return customRoles;
	}

	/**
	 * This method is used to form the payload which is required in the request body
	 * of creating user role in Apigee Management api.
	 * 
	 * @param roleName The name of the role in organization
	 */
	public static String customRoleConversion(String roleName) {
		return " { \"role\" : [ {\"name\" :\"" + roleName + "\"}]} ";
	}

	/**
	 * This method is used to form the payload which is required in the request body
	 * of Adding Multiple Resource permission in Apigee Management api
	 * 
	 * @param permissions The required permissions to be assigned to a role
	 */
	public static String customRolePermissionConversion(List<ResourcePermission> permissions) {
		Gson gson = new Gson();
		return "{\"resourcePermission\":" + gson.toJson(permissions) + "}";
	}

}
