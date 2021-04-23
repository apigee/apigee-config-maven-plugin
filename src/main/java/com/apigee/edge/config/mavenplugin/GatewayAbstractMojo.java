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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultProxyAuthenticationHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apigee.edge.config.utils.ConfigReader;
import com.apigee.edge.config.utils.ConsolidatedConfigReader;
import com.apigee.edge.config.utils.ServerProfile;

public abstract class GatewayAbstractMojo extends AbstractMojo implements Contextualizable {
	
	static Logger logger = LoggerFactory.getLogger(GatewayAbstractMojo.class);
	protected static final Pattern URL_PARSE_REGEX = Pattern.compile("^(http[s]?)://([^:/?#]*).*$");

	/**
	 * The project being built
	 *
	 * @parameter default-value="${project}"
	 * @readonly
	 * @required
	 */
	protected MavenProject project;

	/**
	 * The Maven session
	 *
	 * @parameter default-value="${session}"
	 * @readonly
	 */
	protected MavenSession session;

	/**
	 * The Maven settings
	 *
	 * @parameter default-value="${settings}"
	 * @readonly
	 */
	protected Settings settings;

	/**
	 * Injecting the underlying IoC container to access maven configuration.
	 */
	@Requirement
	protected PlexusContainer container;

	/**
	 * Injecting the settings decrypter module that allows us to access decrypted properties.
	 */
	@Requirement
	protected SettingsDecrypter settingsDecrypter;

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
	
	/**
	 * configuration file
	 * @parameter property="apigee.config.file"
 	 */
	private String configFilePath;
	
	/**
	 * kvm override
	 * @parameter property="apigee.kvm.override" default-value="true"
 	 */
	private String kvmOverride;
	
	
	/**
	 * Parameter to set for DeveloperApp to ignore API Product so that new credentials are not generated for updates (https://github.com/apigee/apigee-config-maven-plugin/issues/17)
	 * @parameter property="apigee.app.ignoreAPIProducts" default-value="false"
 	 */
	private String ignoreProductsForApp;
	
	// TODO set resources/edge as default value

	/**
	 * {@inheritDoc}
	 */
	public void contextualize(Context context) throws ContextException {
		container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
		if (container.hasComponent(SettingsDecrypter.class)) {
			try {
				settingsDecrypter = container.lookup(SettingsDecrypter.class);
			} catch (ComponentLookupException e) {
				logger.warn("Failed to lookup build in maven component session descrupter.", e);
			}
		}
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
		this.buildProfile.setKvmOverride(this.kvmOverride);
		this.buildProfile.setIgnoreProductsForApp(this.ignoreProductsForApp);
		
		// process proxy for management api endpoint
		Proxy mavenProxy = getProxy(settings, hostURL);
		if (mavenProxy != null) {
			logger.info("set proxy to " + mavenProxy.getHost() + ":" + mavenProxy.getPort());
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpHost proxy = new HttpHost(mavenProxy.getHost(), mavenProxy.getPort());
			httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
			if (isNotBlank(mavenProxy.getNonProxyHosts())) {
				//System.setProperty("http.nonProxyHosts", mavenProxy.getNonProxyHosts().replaceAll("[,;]", "|"));
				// TODO selector based proxy
			}
			if (isNotBlank(mavenProxy.getUsername()) && isNotBlank(mavenProxy.getPassword())) {
				logger.debug("set proxy credentials");
				httpClient.setProxyAuthenticationHandler(new DefaultProxyAuthenticationHandler());
				httpClient.getCredentialsProvider().setCredentials(
						new AuthScope(mavenProxy.getHost(), mavenProxy.getPort()),
						new UsernamePasswordCredentials(mavenProxy.getUsername(), mavenProxy.getPassword()));
				buildProfile.setProxyUsername(mavenProxy.getUsername());
				buildProfile.setProxyPassword(mavenProxy.getPassword());
			}
			buildProfile.setApacheHttpClient(httpClient);
			
			//Set Proxy configurations
			buildProfile.setHasProxy(true);
			buildProfile.setProxyProtocol(mavenProxy.getProtocol());
			buildProfile.setProxyServer(mavenProxy.getHost());
			buildProfile.setProxyPort(mavenProxy.getPort());
		}
		
		return buildProfile;
	}
	
	/**
	 *  Get the proxy configuration from the maven settings
	 * 
	 *  @param settings the maven settings   
	 *  @param host the host name of the apigee edge end point
	 *  @return proxy or null if none was configured or the host was non-proxied      
	 */
	protected Proxy getProxy(final Settings settings, final String host) {
		if (settings == null) {
			return null;
		}
		List<Proxy> proxies = settings.getProxies();
		if (proxies == null || proxies.isEmpty()) {
			return null;
		}

		String protocol = "https";
		String hostname = host;

		// check if protocol is present, if not assume https
		Matcher matcher = URL_PARSE_REGEX.matcher(host);
		if (matcher.matches()) {
			protocol = matcher.group(1);
			hostname = matcher.group(2);
		}

		// search active proxy
		for (Proxy proxy : proxies) {
			if (proxy.isActive() && protocol.equalsIgnoreCase(proxy.getProtocol()) && !matchNonProxy(proxy, hostname)) {
				if (settingsDecrypter != null) {
					return settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(proxy)).getProxy();
				} else {
					logger.warn("Maven did not inject SettingsDecrypter, " +
							"proxy may contain an encrypted password, which cannot be " +
							"used to setup the REST client.");
					return proxy;
				}
			}
		}
		return null;
	}

	/**
	 * Check hostname that matched nonPRoxu setting
	 * @param proxy	 Maven Proxy must not be null
	 * @param hostname 
	 * @return
	 */
	protected boolean matchNonProxy(final Proxy proxy, final String hostname) {

		// code from org.apache.maven.plugins.site.AbstractDeployMojo#getProxyInfo
		final String nonProxyHosts = proxy.getNonProxyHosts();
		if (null != nonProxyHosts) {
			final String[] nonProxies = nonProxyHosts.split("(,)|(;)|(\\|)");
			if (null != nonProxies) {
				for (final String nonProxyHost : nonProxies) {
					//if ( StringUtils.contains( nonProxyHost, "*" ) )
					if (null != nonProxyHost && nonProxyHost.contains("*")) {
						// Handle wildcard at the end, beginning or middle of the nonProxyHost
						final int pos = nonProxyHost.indexOf('*');
						String nonProxyHostPrefix = nonProxyHost.substring(0, pos);
						String nonProxyHostSuffix = nonProxyHost.substring(pos + 1);
						// prefix*
						if (!isBlank(nonProxyHostPrefix) && hostname.startsWith(nonProxyHostPrefix) && isBlank(nonProxyHostSuffix)) {
							return true;
						}
						// *suffix
						if (isBlank(nonProxyHostPrefix) && !isBlank(nonProxyHostSuffix) && hostname.endsWith(nonProxyHostSuffix)) {
							return true;
						}
						// prefix*suffix
						if (!isBlank(nonProxyHostPrefix) && hostname.startsWith(nonProxyHostPrefix)
								&& !isBlank(nonProxyHostSuffix) && hostname.endsWith(nonProxyHostSuffix)) {
							return true;
						}
					} else if (hostname.equals(nonProxyHost)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private File findConsolidatedConfigFile()
			throws MojoExecutionException {
		File fConfigFile = null;
		if(configFilePath !=null && !configFilePath.equalsIgnoreCase("")){
			fConfigFile = new File(configFilePath);
		}else{
			fConfigFile = new File(getBaseDirectoryPath() + File.separator +"edge.json");
		}
		if (fConfigFile.exists()) {
			return fConfigFile;
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
			logger.info("No edge.json or config file found.");
			throw new MojoExecutionException("config file edge.json or config file not found");
		}

		logger.info("Retrieving config from " + configFile.getAbsolutePath());
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
			logger.info("No edge.json or config file found.");
			throw new MojoExecutionException("config file edge.json or config file not found");
		}

		logger.info("Retrieving config from " + configFile.getAbsolutePath());
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
			logger.info("No edge.json or config file found.");
			throw new MojoExecutionException("config file edge.json or config file not found");
		}

		logger.info("Retrieving config from " + configFile.getAbsolutePath());
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
			logger.info("No edge.json or config file found.");
			throw new MojoExecutionException("config file edge.json or config file not found");
		}

		logger.info("Retrieving config from " + configFile.getAbsolutePath());
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
			logger.info("No edge.json or config file found.");
			throw new MojoExecutionException("config file edge.json or config file not found");
		}

		logger.info("Retrieving config from " + configFile.getAbsolutePath());
		try {
			return ConsolidatedConfigReader.getOrgConfigWithId(configFile,
					"orgConfig",
					config);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
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

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public boolean isSkip() {
		return skip;
	}
	
	public void setSkip(boolean skip) {
		this.skip = skip;
	}
	
	public String getKvmOverride() {
		return kvmOverride;
	}

	public void setKvmOverride(String kvmOverride) {
		this.kvmOverride = kvmOverride;
	}
	
	public String getExportDir() {
		return exportDir;
	}

	public void setExportDir(String exportDir) {
		this.exportDir = exportDir;
	}
	
	public String getIgnoreProductsForApp() {
		return ignoreProductsForApp;
	}

	public void setIgnoreProductsForApp(String ignoreProductsForApp) {
		this.ignoreProductsForApp = ignoreProductsForApp;
	}
	
}
