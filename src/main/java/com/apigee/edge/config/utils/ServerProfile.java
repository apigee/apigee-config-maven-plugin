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
package com.apigee.edge.config.utils;

import com.apigee.edge.config.rest.RestUtil;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

import org.apache.http.client.HttpClient;
import org.apache.maven.plugin.MojoFailureException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

public class ServerProfile {

	private static Logger logger = LoggerFactory.getLogger(ServerProfile.class);

	private String application; // application name
	private String org;
	private String credential_user;
	private String credential_pwd; //
	private String hostURL; // hostname & scheme e.g.,
							// https://api.enterprise.apigee.com
	private String environment; // prod or test
	private String api_version; // v2 or v1 in the server url
	private String bundle_zip_full_path;
	private String profileId; //Profile id as in parent pom
	private String options;
	private Boolean cpsEnabled;
	
	private String tokenURL; // Mgmt API OAuth token endpoint
	private String mfaToken; // Mgmt API OAuth MFA - TOTP
	private String clientId; //Mgmt API OAuth Client Id (optional)
	private String clientSecret; //Mgmt API OAuth Client Secret (optional)
	private String bearerToken; //Mgmt API OAuth Token
	private String refreshToken; //Mgmt API OAuth Refresh Token
	private String authType; // Mgmt API Auth Type oauth|basic
	private Boolean kvmOverride = true; //Override kvm only if true (used for update option)
	private Boolean ignoreProductsForApp = true; //Ignore API Product for App creation/updates so new credentials are not created
	
	private HttpClient apacheHttpClient;
	
	//For Proxy
	private boolean hasProxy;
	private String proxyProtocol;
	private String proxyServer;
	private int proxyPort;
	private String proxyUsername;
	private String proxyPassword;
	
	public Boolean getKvmOverride() {
		return kvmOverride;
	}

	public void setKvmOverride(String kvmOverride) {
		if(kvmOverride == null || kvmOverride.equals("true"))
			this.kvmOverride = true;
		else if (kvmOverride.equals("false"))
			this.kvmOverride = false;
	}
	
	public Boolean getIgnoreProductsForApp() {
		return ignoreProductsForApp;
	}

	public void setIgnoreProductsForApp(String ignoreProductsForApp) {
		if(ignoreProductsForApp != null && ignoreProductsForApp.equals("true"))
			this.ignoreProductsForApp = true;
		else 
			this.ignoreProductsForApp = false;
	}
	
	public String getHostURL() {
		return hostURL;
	}

	public void setHostURL(String hostURL) {
		this.hostURL = hostURL;
	}

	public String getTokenUrl() {
		return tokenURL;
	}

	public void setTokenUrl(String tokenURL) {
		this.tokenURL = tokenURL;
	}

	public String getMFAToken() {
		return mfaToken;
	}

	public void setMFAToken(String mfaToken) {
		this.mfaToken = mfaToken;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getBearerToken() {
		return bearerToken;
	}

	public void setBearerToken(String bearerToken) {
		this.bearerToken = bearerToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getAuthType() {
		return authType;
	}

	public void setAuthType(String authType) {
		this.authType = authType;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public String getCredential_user() {
		return credential_user;
	}

	public void setCredential_user(String credential_user) {
		this.credential_user = credential_user;
	}

	public String getCredential_pwd() {
		return credential_pwd;
	}

	public void setCredential_pwd(String credential_pwd) {
		this.credential_pwd = credential_pwd;
	}

	public String getHostUrl() {
		return hostURL;
	}

	public void setHostUrl(String host) {
		this.hostURL = host;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getApi_version() {
		return api_version;
	}

	public void setApi_version(String api_version) {
		this.api_version = api_version;
	}

	public String getBundle_zip_full_path() {
		return bundle_zip_full_path;
	}

	public void setBundle_zip_full_path(String bundle_zip_full_path) {
		this.bundle_zip_full_path = bundle_zip_full_path;
	}

	/**
	 * @return the profileid
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * @param id the id to set
	 */
	public void setProfileId(String id) {
		this.profileId = id;
	}

	/**
	 * @return options the options to set
	 */
	
	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}
	
	public HttpClient getApacheHttpClient() {
		return apacheHttpClient;
	}

	public void setApacheHttpClient(HttpClient apacheHttpClient) {
		this.apacheHttpClient = apacheHttpClient;
	}
	
	/**
	 * @return the proxyProtocol
	 */
	public String getProxyProtocol() {
		return proxyProtocol;
	}

	/**
	 * @param proxyProtocol the proxyProtocol to set
	 */
	public void setProxyProtocol(String proxyProtocol) {
		this.proxyProtocol = proxyProtocol;
	}

	/**
	 * @return the proxyServer
	 */
	public String getProxyServer() {
		return proxyServer;
	}

	/**
	 * @param proxyServer the proxyServer to set
	 */
	public void setProxyServer(String proxyServer) {
		this.proxyServer = proxyServer;
	}

	/**
	 * @return the proxyPort
	 */
	public int getProxyPort() {
		return proxyPort;
	}

	/**
	 * @param proxyPort the proxyPort to set
	 */
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	/**
	 * @return the hasProxy
	 */
	public boolean getHasProxy() {
		return hasProxy;
	}

	/**
	 * @param hasProxy the hasProxy to set
	 */
	public void setHasProxy(boolean hasProxy) {
		this.hasProxy = hasProxy;
	}

	/**
	 * @return the proxyUsername
	 */
	public String getProxyUsername() {
		return proxyUsername;
	}

	/**
	 * @param proxyUsername the proxyUsername to set
	 */
	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}
	
	/**
	 * @return the proxyPassword
	 */
	public String getProxyPassword() {
		return proxyPassword;
	}

	/**
	 * @param proxyPassword the proxyPassword to set
	 */
	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}
	
	/**
	 * @return cpsEnabled is CPS org
	 */
	public Boolean getCpsEnabled() throws MojoFailureException {
		if (this.cpsEnabled == null) {
			try {
				HashMap<String,String> features = queryOrgFeatures(this);
				if (features != null &&
						features.get("features.isCpsEnabled") != null &&
						features.get("features.isCpsEnabled").equals("true")) {
					this.cpsEnabled = new Boolean(true);
				} else {
					this.cpsEnabled = new Boolean(false);
				}
			} catch (Exception e) {
				throw new MojoFailureException(
						"Error finding org features. Check if user can access /organizations");
			}
		}

		return this.cpsEnabled;
	}

	private HashMap<String,String> queryOrgFeatures(ServerProfile profile)
			throws IOException {
		RestUtil restUtil = new RestUtil(profile);
		HttpResponse response = restUtil.getOrgConfig(profile, "");
		try {
            assert response != null;
            logger.debug("response Get Organization: " + response.getContentType());
			// response can be read only once
			String jsonRespnse = response.parseAsString();
			logger.debug(jsonRespnse);

			JSONParser parser = new JSONParser();
			JSONObject jsonPayload = (JSONObject)parser.parse(jsonRespnse);
			JSONObject properties = (JSONObject)jsonPayload.get("properties");
			if (properties == null) return null;

			JSONArray property  = (JSONArray)properties.get("property");
			if (property == null) return null;

			HashMap<String,String> organizationProfileMap = new HashMap<String,String>();
			for (Object obj: property) {
				JSONObject feature = (JSONObject)obj;
				String name = (String)feature.get("name");
				String value = (String)feature.get("value");
				if (name != null) {
					organizationProfileMap.put(name, value);
				}
			}
			return organizationProfileMap;

		} catch (ParseException pe){
			logger.error("Org properties check error " + pe.getMessage());
			throw new IOException(pe.getMessage());
		} catch (HttpResponseException e) {
			logger.error("Org properties check error " + e.getMessage());
			throw new IOException(e.getMessage());
		}
	}

}
