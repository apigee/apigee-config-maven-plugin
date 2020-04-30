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
 * Goal to create User Roles in Apigee EDGE. scope: org
 *
 * @author Abhishek Chouksey
 * @author Pallavi Tanpure
 * @author Soudnya Nalawade
 * @author Himanshu Sachdev
 * @goal userroles
 * @phase install
 */

public class UserRoleMojo extends GatewayAbstractMojo {
	static Logger logger = LoggerFactory.getLogger(UserRoleMojo.class);
	public static final String ____ATTENTION_MARKER____ = "************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

	public static class UserRole {
		@Key
		public String name;
		public List<ResourcePermission> resourcepermissions;
	}

	public class ResourcePermission {

		String path;
		String[] permissions;
	}

	public UserRoleMojo() {
		super();

	}

	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee User Roles");
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

	protected String getUserRoleName(String payload) throws MojoFailureException {
		Gson gson = new Gson();
		try {
			UserRole userrole = gson.fromJson(payload, UserRole.class);
			return userrole.name;

		} catch (JsonParseException e) {
			throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(List<String> userRoles) throws MojoFailureException {
		try {
			List existingUserRoles = null;
			if (buildOption != OPTIONS.update && buildOption != OPTIONS.create && buildOption != OPTIONS.delete
					&& buildOption != OPTIONS.sync) {
				return;
			}

			logger.info("Retrieving existing environment User Roles - ");
			existingUserRoles = getUserRoles(serverProfile);

			for (String userRole : userRoles) {
				String userRoleName = getUserRoleName(userRole);
				if (userRoleName == null) {
					throw new IllegalArgumentException("User Role does not have a name.\n" + userRole + "\n");
				}

				if (existingUserRoles.contains(userRoleName)) {
					switch (buildOption) {
					case update:
						logger.info("User Role \"" + userRoleName + "\" exists. Updating.");
						updateUserRole(serverProfile, userRole);
						break;
					case create:
						logger.info("User Role \"" + userRoleName + "\" already exists. Skipping.");
						break;
					case delete:
						logger.info("User Role \"" + userRoleName + "\" already exists. Deleting.");
						deleteUserRole(serverProfile, userRoleName);
						break;
					case sync:
						logger.info("User Role \"" + userRoleName + "\" already exists. Deleting and recreating.");
						deleteUserRole(serverProfile, userRoleName);
						logger.info("Creating User Role - " + userRoleName);
						createUserRole(serverProfile, userRole);
						break;
					}
				} else {
					switch (buildOption) {
					case create:
					case sync:
					case update:
						logger.info("Creating User Role - " + userRoleName);
						createUserRole(serverProfile, userRole);
						break;
					case delete:
						logger.info("User Role \"" + userRoleName + "\" does not exist. Skipping.");
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

		Logger logger = LoggerFactory.getLogger(UserRoleMojo.class);

		try {

			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping User Roles (default action)");
				return;
			}

			if (serverProfile.getEnvironment() == null) {
				throw new MojoExecutionException("Apigee environment not found in profile");
			}
			List userRoles = getOrgConfig(logger, "userroles");
			if (userRoles == null || userRoles.size() == 0) {
				logger.info("No User Role config found.");
				return;
			}

			doUpdate(userRoles);

		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

	/***************************************************************************
	 * REST call wrappers
	 **/
	public static String createUserRole(ServerProfile profile, String userRole) throws IOException {

		Gson gson = new Gson();
		UserRole userRoleObject = gson.fromJson(userRole, UserRole.class);
		String namePayload = userRoleConversion(userRoleObject.name);
		RestUtil restUtil = new RestUtil(profile);
		HttpResponse response = restUtil.createOrgConfig(profile, "userroles", namePayload);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode()) {
				logger.info("Create Success for User Role.");
				addResourcePermissionsToRole(profile, userRoleObject);
			}

		} catch (HttpResponseException e) {
			logger.error("User Role create error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}

	/**
	 * This method Assign Multiple Resource Permissions to the user role .
	 * 
	 * @param profile
	 * @param userRoleObject user role Object
	 *
	 */
	private static void addResourcePermissionsToRole(ServerProfile profile, UserRole userRoleObject)
			throws IOException {
		RestUtil restUtil = new RestUtil(profile);
		String permissionsPayload = userRolePermissionConversion(userRoleObject.resourcepermissions);
		HttpResponse response = restUtil.createOrgConfig(profile,
				"userroles" + "/" + userRoleObject.name + "/resourcepermissions", permissionsPayload);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode()) {
				logger.info("Create Success for role permissions.");
			}

		} catch (HttpResponseException e) {
			logger.error("User Role create error " + e.getMessage());
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * This method is used to update the user role Permissions.
	 *
	 * @param profile
	 * @param userRole
	 *
	 */
	public static String updateUserRole(ServerProfile profile, String userRole) throws IOException {
		Gson gson = new Gson();
		UserRole userRoleObject = gson.fromJson(userRole, UserRole.class);
		addResourcePermissionsToRole(profile, userRoleObject);
		return "";
	}

	public static String deleteUserRole(ServerProfile profile, String userRoleName) throws IOException {
		RestUtil restUtil = new RestUtil(profile);
		HttpResponse response = restUtil.deleteOrgConfig(profile, "userroles", userRoleName);
		try {

			logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
			if (response.isSuccessStatusCode())
				logger.info("Delete Success.");

		} catch (HttpResponseException e) {
			logger.error("User Role delete error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return "";
	}

	public static List getUserRoles(ServerProfile profile) throws IOException {
		RestUtil restUtil = new RestUtil(profile);
		HttpResponse response = restUtil.getOrgConfig(profile, "userroles");
		if (response == null)
			return new ArrayList();
		JSONArray userRoles = null;
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
			String obj = "{ \"userRoles\": " + payload + "}";

			JSONParser parser = new JSONParser();
			JSONObject obj1 = (JSONObject) parser.parse(obj);
			userRoles = (JSONArray) obj1.get("userRoles");

		} catch (ParseException pe) {
			logger.error("Get User Role parse error " + pe.getMessage());
			throw new IOException(pe.getMessage());
		} catch (HttpResponseException e) {
			logger.error("Get User Role error " + e.getMessage());
			throw new IOException(e.getMessage());
		}

		return userRoles;
	}

	/**
	 * This method is used to form the payload which is required in the request body
	 * of creating user role in Apigee Management api.
	 * 
	 * @param roleName The name of the role in organization
	 */
	public static String userRoleConversion(String roleName) {
		return " { \"role\" : [ {\"name\" :\"" + roleName + "\"}]} ";
	}

	/**
	 * This method is used to form the payload which is required in the request body
	 * of Adding Multiple Resource permission in Apigee Management api
	 * 
	 * @param permissions The required permissions to be assigned to a role
	 */
	public static String userRolePermissionConversion(List<ResourcePermission> permissions) {
		Gson gson = new Gson();
		return "{\"resourcePermission\":" + gson.toJson(permissions) + "}";
	}

}
