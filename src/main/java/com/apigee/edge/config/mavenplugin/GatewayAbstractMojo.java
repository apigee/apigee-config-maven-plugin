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

import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.apigee.edge.config.utils.ServerProfile;
import com.apigee.edge.config.utils.ConfigReader;
import com.apigee.edge.config.utils.ConsolidatedConfigReader;
import org.apache.maven.plugin.AbstractMojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

public abstract class GatewayAbstractMojo extends AbstractMojo {

	/**
	 * Directory containing the build files.
	 * 
	 * @parameter property="project.build.directory"
	 */
	private File buildDirectory;
	
	/**
	 * Base directory of the project.
	 * 
	 * @parameter property="basedir"
	 */
	private File baseDirectory;

	/**
	 * Project Name
	 * 
	 * @parameter property="project.name"
	 */
	private String projectName;
	
	/**
	 * Project version
	 * 
	 * @parameter property="project.version"
	 */
	private String projectVersion;

	/**
	 * Project artifact id
	 * 
	 * @parameter property="project.artifactId"
	 */
	private String artifactId;
	
	/**
	 * Profile id
	 * 
	 * @parameter property="apigee.profile"
	 */
	private String id;
	

	/**
	 * Gateway host URL
	 * 
	 * @parameter property="apigee.hosturl"
	 */
	private String hostURL;
	

	/**
	 * Gateway env profile
	 * 
	 * @parameter property="apigee.env" default-value="${apigee.profile}"
	 */
	private String deploymentEnv;
	
	/**
	 * Gateway api version
	 * 
	 * @parameter property="apigee.apiversion"
	 */
	private String apiVersion;
	
	
	/**
	 * Gateway org name
	 * 
	 * @parameter property="apigee.org"
	 */
	private String orgName;
	
	/**
	 * Gateway host username
	 * 
	 * @parameter property="apigee.username"
	 */
	private String userName;
	
	/**
	 * Gateway host password
	 * 
	 * @parameter property="apigee.password"
	 */
	private String password;

	/**
	 * Build option
	 * 
	 * @parameter property="build.option"
	 */
	private String buildOption;
	
	
	/**
	 * Gateway options
	 * 
	 * @parameter property="apigee.config.options"
	 */
	private String options;

	/**
	 * Config dir
	 * @parameter property="apigee.config.dir"
 	 */
	private String configDir;
	
	/**
	 * Export dir for Apigee Dev App Keys
	 * 
	 * @parameter property="apigee.config.exportDir"
	 */
	private String exportDir;
	
	/**
	 * Mgmt API OAuth token endpoint
	 * 
	 * @parameter expression="${apigee.tokenurl}" default-value="https://login.apigee.com/oauth/token"
	 */
	private String tokenURL;

	/**
	 * Mgmt API OAuth MFA - TOTP
	 * 
	 * @parameter expression="${apigee.mfatoken}"
	 */
	private String mfaToken;

	/**
	 * Mgmt API authn type
	 * 
	 * @parameter expression="${apigee.authtype}" default-value="basic"
	 */
	private String authType;
	
	/**
	 * Gateway bearer token
	 * 
	 * @parameter expression="${apigee.bearer}"
	 */
	private String bearer;
	
	/**
	 * Gateway refresh token
	 * 
	 * @parameter expression="${apigee.refresh}"
	 */
	private String refresh;
	
	/**
	 * Gateway OAuth clientId
	 * 
	 * @parameter expression="${apigee.clientid}"
	 */
	private String clientid;
	
	/**
	 * Gateway OAuth clientSecret
	 * 
	 * @parameter expression="${apigee.clientsecret}"
	 */
	private String clientsecret;
	
	// TODO set resources/edge as default value

	public String getExportDir() {
		return exportDir;
	}

	public void setExportDir(String exportDir) {
		this.exportDir = exportDir;
	}

	/**
	* Skip running this plugin.
	* Default is false.
	*
	* @parameter default-value="false"
	*/
	private boolean skip = false;

	public ServerProfile buildProfile;

	public GatewayAbstractMojo(){
		super();
		
	}

	public ServerProfile getProfile() {
		this.buildProfile = new ServerProfile();
		this.buildProfile.setOrg(this.orgName);
		this.buildProfile.setApplication(this.projectName);
		this.buildProfile.setApi_version(this.apiVersion);
		this.buildProfile.setHostUrl(this.hostURL);
		this.buildProfile.setEnvironment(this.deploymentEnv);
		this.buildProfile.setCredential_user(this.userName);
		this.buildProfile.setCredential_pwd(this.password);
		this.buildProfile.setProfileId(this.id);
		this.buildProfile.setOptions(this.options);
		this.buildProfile.setTokenUrl(this.tokenURL);
		this.buildProfile.setMFAToken(this.mfaToken);
		this.buildProfile.setAuthType(this.authType);
		this.buildProfile.setBearerToken(this.bearer);
		this.buildProfile.setRefreshToken(this.refresh);
		this.buildProfile.setClientId(this.clientid);
		this.buildProfile.setClientSecret(this.clientsecret);
		return buildProfile;
	}

	public void setProfile(ServerProfile profile) {
		this.buildProfile = profile;
	}

	public void setBaseDirectory(File baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	public String getBuildDirectory() {
		return this.buildDirectory.getAbsolutePath(); 
	}

	public String getBaseDirectoryPath(){
		return this.baseDirectory.getAbsolutePath();
	}

	public String getBuildOption() {
		return buildOption;
	}

	public void setBuildOption(String buildOption) {
		this.buildOption = buildOption;
	}

	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}


	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}


	public boolean isSkip() {
		return skip;
	}


	public void setSkip(boolean skip) {
		this.skip = skip;
	}

	private File findConsolidatedConfigFile()
			throws MojoExecutionException {
		File configFile = new File(getBaseDirectoryPath() + File.separator +
									"edge.json");
		if (configFile.exists()) {
			return configFile;
		}
		return null;
	}

	private File findConfigFile(String scope, String config)
			throws MojoExecutionException {
		File configFile = new File(configDir + File.separator +
									scope + File.separator +
									config + ".json");
		if (configFile.exists()) {
			return configFile;
		}
		return null;
	}

	protected List getAPIConfig(Logger logger, String config, String api)
			throws MojoExecutionException {
		File configFile;
		String scope = "api" + File.separator + api;

		/* configDir takes precedence over edge.json */
		if (configDir != null && configDir.length() > 0) {
			configFile = findConfigFile(scope, config);
			if (configFile == null) {
				logger.info("Config file " + scope + File.separator + config + ".json not found.");
				return null;
			}

			logger.info("Retrieving config from " + scope + File.separator + config + ".json");
			try {
				return ConfigReader.getAPIConfig(configFile);
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage());
			}
		}

		/* consolidated edge.json in CWD as fallback */
		configFile = findConsolidatedConfigFile();

		if (configFile == null) {
			logger.info("No edge.json found.");
			throw new MojoExecutionException("config file edge.json not found");
		}

		logger.debug("Retrieving config from edge.json");
		try {

			return ConsolidatedConfigReader.getAPIConfig(configFile,
					api,
					config);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

	protected Set<String> getAPIList(Logger logger)
			throws MojoExecutionException {
		File configFile;
		String scope = configDir + File.separator + "api";

		/* configDir takes precedence over edge.json */
		if (configDir != null && configDir.length() > 0) {
			logger.info("Retrieving API list from " + scope);
			try {
				return ConfigReader.getAPIList(scope);
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage());
			}
		}

		/* consolidated edge.json in CWD as fallback */
		configFile = findConsolidatedConfigFile();

		if (configFile == null) {
			logger.info("No edge.json found.");
			throw new MojoExecutionException("config file edge.json not found");
		}

		logger.debug("Retrieving config from edge.json");
		try {
			return ConsolidatedConfigReader.getAPIList(configFile);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

	/*
	*  env picked from maven profile
	*  No support for maven profile names itself */
	protected List getEnvConfig(Logger logger, String config)
			throws MojoExecutionException {
		File configFile;
		String scope = "env" + File.separator + this.buildProfile.getEnvironment();

		/* configDir takes precedence over edge.json */
		if (configDir != null && configDir.length() > 0) {
			configFile = findConfigFile(scope, config);
			if (configFile == null) {
				logger.info("Config file " + scope + File.separator + config + ".json not found.");
				return null;
			}

			logger.info("Retrieving config from " + scope + File.separator + config + ".json");
			try {
				return ConfigReader.getEnvConfig(this.buildProfile.getEnvironment(),
													configFile);
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage());
			}
		}

		/* consolidated edge.json in CWD as fallback */
		configFile = findConsolidatedConfigFile();

		if (configFile == null) {
			logger.info("No edge.json found.");
			throw new MojoExecutionException("config file edge.json not found");
		}

		logger.debug("Retrieving config from edge.json");
		try {
			List envConfigs = ConsolidatedConfigReader.getEnvConfig(
					this.buildProfile.getEnvironment(),
							configFile,
							"envConfig",
							config);
			return envConfigs;
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

	protected List getOrgConfig(Logger logger, String config)
			throws MojoExecutionException {
		File configFile;
		String scope = "org";

		/* configDir takes precedence over edge.json */
		if (configDir != null && configDir.length() > 0) {
			configFile = findConfigFile(scope, config);
			if (configFile == null) {
				logger.info("Config file " + scope + File.separator + config + ".json not found.");
				return null;
			}

			logger.info("Retrieving config from " + scope + File.separator + config + ".json");
			try {
				return ConfigReader.getOrgConfig(configFile);
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage());
			}
		}

		/* consolidated edge.json in CWD as fallback */
		configFile = findConsolidatedConfigFile();

		if (configFile == null) {
			logger.info("No edge.json found.");
			throw new MojoExecutionException("config file edge.json not found");
		}

		logger.debug("Retrieving config from edge.json");
		try {
			return ConsolidatedConfigReader.getOrgConfig(configFile,
															"orgConfig",
															config);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

	protected Map getOrgConfigWithId(Logger logger, String config)
			throws MojoExecutionException {
		File configFile;
		String scope = "org";

		/* configDir takes precedence over edge.json */
		if (configDir != null && configDir.length() > 0) {
			configFile = findConfigFile(scope, config);
			if (configFile == null) {
				logger.info("Config file " + scope + File.separator + config + ".json not found.");
				return null;
			}

			logger.info("Retrieving config from " + scope + File.separator + config + ".json");
			try {
				return ConfigReader.getOrgConfigWithId(configFile);
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage());
			}
		}

		/* consolidated edge.json in CWD as fallback */
		configFile = findConsolidatedConfigFile();

		if (configFile == null) {
			logger.info("No edge.json found.");
			throw new MojoExecutionException("config file edge.json not found");
		}

		logger.debug("Retrieving config from edge.json");
		try {
			return ConsolidatedConfigReader.getOrgConfigWithId(configFile,
					"orgConfig",
					config);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

}
